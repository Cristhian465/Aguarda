-- Reportes de la comunidad: "sin agua en mi cuadra", "llegó la cisterna", etc.
-- Se ejecuta una sola vez en Supabase: SQL Editor > New query > pegar este archivo > Run.
-- Requiere haber ejecutado antes liga_del_agua.sql (reutiliza el mismo código anónimo del hogar).
--
-- Privacidad: solo se guarda un código anónimo, el tipo de reporte, el distrito y, si es de una
-- cisterna, cuál. Nunca se guarda la ubicación del vecino. Nadie puede leer la tabla directamente:
-- la app solo usa las funciones de abajo. Un reporte se muestra cuando al menos 2 hogares distintos
-- coinciden, y se olvida a los 2 días.

create table if not exists public.reporte_agua (
    id         bigint generated always as identity primary key,
    hogar_hash text        not null,
    tipo       text        not null check (tipo in ('sin_agua', 'volvio_agua', 'cisterna_llego', 'cisterna_se_fue')),
    ubigeo     text        not null check (ubigeo in ('230101', '230102', '230104', '230108', '230110')),
    punto_id   text        check (punto_id ~ '^PC-[0-9]{2}$'),
    creado_en  timestamptz not null default now(),
    check ((tipo like 'cisterna%') = (punto_id is not null))
);

create index if not exists reporte_agua_creado_en_idx on public.reporte_agua (creado_en);
create index if not exists reporte_agua_hogar_idx on public.reporte_agua (hogar_hash, creado_en);

alter table public.reporte_agua enable row level security;
revoke all on table public.reporte_agua from anon, authenticated;

-- Guarda un reporte. Máximo un reporte igual cada 3 horas y 10 por hora por hogar.
create or replace function public.enviar_reporte(
    p_token uuid, p_tipo text, p_ubigeo text, p_punto text
) returns void
language plpgsql security definer set search_path = ''
as $$
declare
    v_hash text := encode(sha256(convert_to(p_token::text, 'UTF8')), 'hex');
begin
    delete from public.reporte_agua where creado_en < now() - interval '2 days';

    if (select count(*) from public.reporte_agua where hogar_hash = v_hash and creado_en > now() - interval '1 hour') >= 10 then
        raise exception 'Demasiados reportes seguidos. Inténtalo más tarde';
    end if;

    if exists (
        select 1 from public.reporte_agua
        where hogar_hash = v_hash and tipo = p_tipo and ubigeo = p_ubigeo
          and coalesce(punto_id, '') = coalesce(p_punto, '')
          and creado_en > now() - interval '3 hours'
    ) then
        raise exception 'Ya enviaste este reporte hace poco';
    end if;

    insert into public.reporte_agua (hogar_hash, tipo, ubigeo, punto_id)
    values (v_hash, p_tipo, p_ubigeo, p_punto);
end;
$$;

-- Lo que reportan los vecinos en las últimas horas. Solo salen los reportes confirmados por 2 hogares o más.
-- hace_minutos es cuánto pasó desde el último reporte, para que la app no tenga que leer fechas.
drop function if exists public.reportes_recientes(integer);
create function public.reportes_recientes(p_horas integer default 6)
returns table (tipo text, ubigeo text, punto_id text, hogares integer, hace_minutos integer)
language sql stable security definer set search_path = ''
as $$
    select r.tipo, r.ubigeo, r.punto_id,
           count(distinct r.hogar_hash)::integer,
           (extract(epoch from now() - max(r.creado_en)) / 60)::integer
    from public.reporte_agua r
    where r.creado_en > now() - make_interval(hours => least(greatest(p_horas, 1), 24))
    group by r.tipo, r.ubigeo, r.punto_id
    having count(distinct r.hogar_hash) >= 2;
$$;

-- Al salir de la Liga se borra todo lo del hogar: su resumen semanal y sus reportes.
create or replace function public.borrar_mis_datos(p_token uuid)
returns void
language plpgsql security definer set search_path = ''
as $$
declare
    v_hash text := encode(sha256(convert_to(p_token::text, 'UTF8')), 'hex');
begin
    delete from public.liga_resumen where hogar_hash = v_hash;
    delete from public.reporte_agua where hogar_hash = v_hash;
end;
$$;

revoke all on function public.enviar_reporte(uuid, text, text, text) from public;
revoke all on function public.reportes_recientes(integer) from public;
revoke all on function public.borrar_mis_datos(uuid) from public;
grant execute on function public.enviar_reporte(uuid, text, text, text) to anon;
grant execute on function public.reportes_recientes(integer) to anon;
grant execute on function public.borrar_mis_datos(uuid) to anon;
