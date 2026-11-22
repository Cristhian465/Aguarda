-- Modo familia: varios teléfonos del mismo hogar comparten el tanque.
-- Se ejecuta una sola vez en Supabase: SQL Editor > New query > pegar este archivo > Run.
--
-- Sin cuentas: el hogar se identifica con un código secreto que solo conocen sus teléfonos.
-- Supabase guarda el hash del secreto, nunca el secreto, salvo dentro de una invitación,
-- que vence a los 15 minutos y se borra al usarse. Nadie puede leer las tablas directamente:
-- la app solo usa las funciones de abajo.
--
-- Se guarda: el perfil del hogar (tipo y capacidad del depósito, habitantes, hábitos), los llenados
-- y los avisos de "me quedé sin agua" de los últimos meses, y el apodo que cada teléfono elige.
-- Nunca se guarda la ubicación ni datos de contacto.

create table if not exists public.familia_hogar (
    familia_hash text        primary key,
    perfil       jsonb,
    creado_en    timestamptz not null default now(),
    check (perfil is null or pg_column_size(perfil) < 4000)
);

create table if not exists public.familia_miembro (
    familia_hash text        not null references public.familia_hogar (familia_hash) on delete cascade,
    miembro_id   uuid        not null,
    apodo        text        not null check (char_length(apodo) between 1 and 30),
    visto_en     timestamptz not null default now(),
    primary key (familia_hash, miembro_id)
);

create table if not exists public.familia_evento (
    id               uuid        primary key,
    familia_hash     text        not null references public.familia_hogar (familia_hash) on delete cascade,
    tipo             text        not null check (tipo in ('llenado', 'sin_agua')),
    momento          text        not null check (momento ~ '^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}'),
    litros           numeric     not null check (litros >= 0 and litros <= 100000),
    inicio_observado text        check (inicio_observado is null or inicio_observado ~ '^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}'),
    autor_id         uuid        not null,
    creado_en        timestamptz not null default now(),
    check ((tipo = 'sin_agua') = (inicio_observado is not null))
);

create index if not exists familia_evento_hogar_idx on public.familia_evento (familia_hash, momento);

create table if not exists public.familia_invitacion (
    codigo       text        primary key,
    familia_hash text        not null references public.familia_hogar (familia_hash) on delete cascade,
    secreto      uuid        not null,
    vence_en     timestamptz not null
);

-- Solo se usa para frenar a quien prueba códigos al azar.
create table if not exists public.familia_intento_fallido (
    creado_en timestamptz not null default now()
);

alter table public.familia_hogar enable row level security;
alter table public.familia_miembro enable row level security;
alter table public.familia_evento enable row level security;
alter table public.familia_invitacion enable row level security;
alter table public.familia_intento_fallido enable row level security;
revoke all on table public.familia_hogar from anon, authenticated;
revoke all on table public.familia_miembro from anon, authenticated;
revoke all on table public.familia_evento from anon, authenticated;
revoke all on table public.familia_invitacion from anon, authenticated;
revoke all on table public.familia_intento_fallido from anon, authenticated;

-- Hash del secreto del hogar. El prefijo lo separa del código anónimo de la Liga.
create or replace function public.familia_hash(p_secreto uuid)
returns text
language sql immutable set search_path = ''
as $$
    select encode(sha256(convert_to('familia:' || p_secreto::text, 'UTF8')), 'hex');
$$;

-- Corta la operación si el teléfono ya no es parte de la familia. La app reconoce el código AF001.
create or replace function public.familia_exigir_miembro(p_hash text, p_miembro uuid)
returns void
language plpgsql set search_path = ''
as $$
begin
    if not exists (
        select 1 from public.familia_miembro where familia_hash = p_hash and miembro_id = p_miembro
    ) then
        raise exception 'Ya no formas parte de esta familia' using errcode = 'AF001';
    end if;
end;
$$;

-- Crea la familia con el perfil del hogar de quien la crea.
create or replace function public.familia_crear(
    p_secreto uuid, p_miembro uuid, p_apodo text, p_perfil jsonb default null
) returns void
language plpgsql security definer set search_path = ''
as $$
declare
    v_hash text := public.familia_hash(p_secreto);
begin
    if (select count(*) from public.familia_hogar where creado_en > now() - interval '1 hour') >= 300 then
        raise exception 'Se están creando demasiadas familias. Inténtalo más tarde';
    end if;
    insert into public.familia_hogar (familia_hash, perfil) values (v_hash, p_perfil);
    insert into public.familia_miembro (familia_hash, miembro_id, apodo) values (v_hash, p_miembro, btrim(p_apodo));
end;
$$;

-- Código de invitación de 10 caracteres. Vence a los 15 minutos y sirve para un solo teléfono.
create or replace function public.familia_invitar(p_secreto uuid, p_miembro uuid)
returns jsonb
language plpgsql security definer set search_path = ''
as $$
declare
    v_hash text := public.familia_hash(p_secreto);
    v_codigo text;
begin
    perform public.familia_exigir_miembro(v_hash, p_miembro);
    delete from public.familia_invitacion where vence_en < now();
    if (select count(*) from public.familia_invitacion where familia_hash = v_hash) >= 5 then
        raise exception 'Ya hay 5 códigos de invitación activos. Espera unos minutos';
    end if;
    v_codigo := upper(substr(replace(gen_random_uuid()::text, '-', ''), 1, 10));
    insert into public.familia_invitacion (codigo, familia_hash, secreto, vence_en)
    values (v_codigo, v_hash, p_secreto, now() + interval '15 minutes');
    return jsonb_build_object('codigo', v_codigo, 'minutos', 15);
end;
$$;

-- Canjea un código de invitación. Devuelve el secreto del hogar o un error, sin lanzar excepción,
-- para que el intento fallido quede registrado.
create or replace function public.familia_unirse(p_codigo text, p_miembro uuid, p_apodo text)
returns jsonb
language plpgsql security definer set search_path = ''
as $$
declare
    v_codigo  text := upper(regexp_replace(coalesce(p_codigo, ''), '[^0-9A-Za-z]', '', 'g'));
    v_familia text;
    v_secreto uuid;
begin
    delete from public.familia_invitacion where vence_en < now();
    delete from public.familia_intento_fallido where creado_en < now() - interval '1 hour';
    if (select count(*) from public.familia_intento_fallido where creado_en > now() - interval '10 minutes') >= 100 then
        return jsonb_build_object('error', 'demasiados_intentos');
    end if;

    delete from public.familia_invitacion where codigo = v_codigo
    returning familia_hash, secreto into v_familia, v_secreto;
    if v_familia is null then
        insert into public.familia_intento_fallido default values;
        return jsonb_build_object('error', 'codigo_invalido');
    end if;

    if (select count(*) from public.familia_miembro where familia_hash = v_familia) >= 10 then
        return jsonb_build_object('error', 'familia_llena');
    end if;

    insert into public.familia_miembro (familia_hash, miembro_id, apodo)
    values (v_familia, p_miembro, btrim(p_apodo))
    on conflict (familia_hash, miembro_id) do update set apodo = excluded.apodo, visto_en = now();
    return jsonb_build_object('secreto', v_secreto);
end;
$$;

-- Sube los eventos nuevos del teléfono (y el perfil si cambió) y devuelve todo lo de la familia
-- desde p_desde (fecha AAAA-MM-DD). Los eventos solo se agregan: un id repetido se ignora.
create or replace function public.familia_sincronizar(
    p_secreto uuid, p_miembro uuid, p_eventos jsonb, p_desde text, p_perfil jsonb default null
) returns jsonb
language plpgsql security definer set search_path = ''
as $$
declare
    v_hash    text := public.familia_hash(p_secreto);
    v_eventos jsonb := coalesce(p_eventos, '[]'::jsonb);
    v_perfil  jsonb;
begin
    perform public.familia_exigir_miembro(v_hash, p_miembro);
    if jsonb_typeof(v_eventos) <> 'array' or jsonb_array_length(v_eventos) > 300 then
        raise exception 'Demasiados eventos en un solo envío';
    end if;

    insert into public.familia_evento (id, familia_hash, tipo, momento, litros, inicio_observado, autor_id)
    select (e ->> 'id')::uuid, v_hash, e ->> 'tipo', e ->> 'momento', (e ->> 'litros')::numeric,
           e ->> 'inicio_observado', p_miembro
    from jsonb_array_elements(v_eventos) as e
    on conflict (id) do nothing;

    if p_perfil is not null then
        update public.familia_hogar set perfil = p_perfil where familia_hash = v_hash;
    end if;

    update public.familia_miembro set visto_en = now() where familia_hash = v_hash and miembro_id = p_miembro;
    delete from public.familia_evento
    where familia_hash = v_hash and momento < to_char(now() - interval '120 days', 'YYYY-MM-DD');

    select perfil into v_perfil from public.familia_hogar where familia_hash = v_hash;

    return jsonb_build_object(
        'perfil', v_perfil,
        'eventos', coalesce((
            select jsonb_agg(jsonb_build_object(
                       'id', ev.id,
                       'tipo', ev.tipo,
                       'momento', ev.momento,
                       'litros', ev.litros,
                       'inicio_observado', ev.inicio_observado,
                       'autor', coalesce(m.apodo, 'Alguien de tu familia'),
                       'propio', ev.autor_id = p_miembro
                   ) order by ev.momento)
            from public.familia_evento ev
            left join public.familia_miembro m on m.familia_hash = ev.familia_hash and m.miembro_id = ev.autor_id
            where ev.familia_hash = v_hash and ev.momento >= p_desde
        ), '[]'::jsonb),
        'miembros', coalesce((
            select jsonb_agg(jsonb_build_object('apodo', m.apodo, 'propio', m.miembro_id = p_miembro) order by m.apodo)
            from public.familia_miembro m
            where m.familia_hash = v_hash
        ), '[]'::jsonb)
    );
end;
$$;

-- El teléfono sale de la familia. Si era el último, se borra todo lo del hogar.
create or replace function public.familia_salir(p_secreto uuid, p_miembro uuid)
returns void
language plpgsql security definer set search_path = ''
as $$
declare
    v_hash text := public.familia_hash(p_secreto);
begin
    delete from public.familia_miembro where familia_hash = v_hash and miembro_id = p_miembro;
    if not exists (select 1 from public.familia_miembro where familia_hash = v_hash) then
        delete from public.familia_hogar where familia_hash = v_hash;
    end if;
end;
$$;

-- Saca a todos los demás y cambia el secreto. El historial se conserva y solo queda quien lo pide.
create or replace function public.familia_reiniciar(
    p_secreto uuid, p_nuevo uuid, p_miembro uuid, p_apodo text
) returns void
language plpgsql security definer set search_path = ''
as $$
declare
    v_hash  text := public.familia_hash(p_secreto);
    v_nuevo text := public.familia_hash(p_nuevo);
begin
    perform public.familia_exigir_miembro(v_hash, p_miembro);
    insert into public.familia_hogar (familia_hash, perfil)
    select v_nuevo, perfil from public.familia_hogar where familia_hash = v_hash;
    update public.familia_evento set familia_hash = v_nuevo where familia_hash = v_hash;
    insert into public.familia_miembro (familia_hash, miembro_id, apodo) values (v_nuevo, p_miembro, btrim(p_apodo));
    delete from public.familia_hogar where familia_hash = v_hash;
end;
$$;

revoke all on function public.familia_hash(uuid) from public;
revoke all on function public.familia_exigir_miembro(text, uuid) from public;
revoke all on function public.familia_crear(uuid, uuid, text, jsonb) from public;
revoke all on function public.familia_invitar(uuid, uuid) from public;
revoke all on function public.familia_unirse(text, uuid, text) from public;
revoke all on function public.familia_sincronizar(uuid, uuid, jsonb, text, jsonb) from public;
revoke all on function public.familia_salir(uuid, uuid) from public;
revoke all on function public.familia_reiniciar(uuid, uuid, uuid, text) from public;
grant execute on function public.familia_crear(uuid, uuid, text, jsonb) to anon;
grant execute on function public.familia_invitar(uuid, uuid) to anon;
grant execute on function public.familia_unirse(text, uuid, text) to anon;
grant execute on function public.familia_sincronizar(uuid, uuid, jsonb, text, jsonb) to anon;
grant execute on function public.familia_salir(uuid, uuid) to anon;
grant execute on function public.familia_reiniciar(uuid, uuid, uuid, text) to anon;

notify pgrst, 'reload schema';
