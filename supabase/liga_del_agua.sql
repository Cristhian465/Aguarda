-- Liga del Agua: ranking semanal de distritos de Tacna.
-- Se ejecuta una sola vez en Supabase: SQL Editor > New query > pegar este archivo > Run.
--
-- Privacidad: solo se guarda un código anónimo, el distrito, la semana, los litros por persona
-- y el puntaje. Nadie puede leer la tabla directamente: la app solo usa las tres funciones de abajo.

create table if not exists public.liga_resumen (
    hogar_hash         text        not null,
    semana             date        not null,
    ubigeo             text        not null check (ubigeo in ('230101', '230102', '230104', '230108', '230110')),
    litros_persona_dia numeric(6,1) not null check (litros_persona_dia between 0 and 1000),
    puntaje            smallint    not null check (puntaje between 0 and 100),
    verificado         boolean     not null default false,
    actualizado_en     timestamptz not null default now(),
    primary key (hogar_hash, semana)
);

alter table public.liga_resumen enable row level security;
revoke all on table public.liga_resumen from anon, authenticated;

-- Guarda o actualiza el resumen del hogar. Solo se acepta la semana en curso (hora de Perú).
create or replace function public.enviar_resumen(
    p_token uuid, p_semana date, p_ubigeo text, p_litros numeric, p_puntaje integer, p_verificado boolean
) returns void
language plpgsql security definer set search_path = ''
as $$
begin
    if p_semana <> date_trunc('week', now() at time zone 'America/Lima')::date then
        raise exception 'Solo se acepta la semana en curso';
    end if;
    insert into public.liga_resumen (hogar_hash, semana, ubigeo, litros_persona_dia, puntaje, verificado)
    values (encode(sha256(convert_to(p_token::text, 'UTF8')), 'hex'), p_semana, p_ubigeo, p_litros, p_puntaje, p_verificado)
    on conflict (hogar_hash, semana) do update
        set ubigeo = excluded.ubigeo,
            litros_persona_dia = excluded.litros_persona_dia,
            puntaje = excluded.puntaje,
            verificado = excluded.verificado,
            actualizado_en = now();
end;
$$;

-- Ranking de la semana. Usa la mediana para resistir datos falsos, y un hogar verificado pesa el doble.
-- Un distrito con menos de 5 hogares no muestra puntaje, para proteger a quienes participan.
create or replace function public.ranking_semana(p_semana date)
returns table (ubigeo text, hogares integer, verificados integer, puntaje numeric, litros_persona_dia numeric)
language sql stable security definer set search_path = ''
as $$
    select r.ubigeo,
           count(distinct r.hogar_hash)::integer,
           (count(distinct r.hogar_hash) filter (where r.verificado))::integer,
           case when count(distinct r.hogar_hash) >= 5
                then round((percentile_cont(0.5) within group (order by r.puntaje))::numeric, 1) end,
           case when count(distinct r.hogar_hash) >= 5
                then round((percentile_cont(0.5) within group (order by r.litros_persona_dia))::numeric, 1) end
    from public.liga_resumen r
    cross join lateral generate_series(1, case when r.verificado then 2 else 1 end) as peso
    where r.semana = p_semana
    group by r.ubigeo;
$$;

-- Borra todo lo que el hogar envió, cuando decide dejar de participar.
create or replace function public.borrar_mis_datos(p_token uuid)
returns void
language sql security definer set search_path = ''
as $$
    delete from public.liga_resumen
    where hogar_hash = encode(sha256(convert_to(p_token::text, 'UTF8')), 'hex');
$$;

revoke all on function public.enviar_resumen(uuid, date, text, numeric, integer, boolean) from public;
revoke all on function public.ranking_semana(date) from public;
revoke all on function public.borrar_mis_datos(uuid) from public;
grant execute on function public.enviar_resumen(uuid, date, text, numeric, integer, boolean) to anon;
grant execute on function public.ranking_semana(date) to anon;
grant execute on function public.borrar_mis_datos(uuid) to anon;
