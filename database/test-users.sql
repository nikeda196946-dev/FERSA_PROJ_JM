-- FERSA Care — vincula contas de teste já criadas no Supabase Auth aos perfis.
-- Primeiro crie as três contas em Authentication → Users → Add user,
-- marque Auto Confirm User e use os e-mails abaixo. Depois execute este arquivo.
-- Defina as senhas no painel; use apenas senhas de teste neste ambiente.

insert into clientes (id, nome)
values ('50000000-0000-0000-0000-000000000001', 'Oficina Trevo Diesel')
on conflict (id) do update set nome = excluded.nome;

insert into profiles (id, nome, perfil, cliente_id)
select u.id, dados.nome, dados.perfil::perfil_usuario, dados.cliente_id
from auth.users u
join (values
  ('mecanico@oficina.com', 'Carlos Mendes', 'MECANICO', '50000000-0000-0000-0000-000000000001'::uuid),
  ('tecnico@fersa.com',   'Renata Souza',  'TECNICO',  null::uuid),
  ('admin@fersa.com',     'Paulo Ferreira','ADMIN',    null::uuid)
) as dados(email, nome, perfil, cliente_id) on lower(u.email) = dados.email
on conflict (id) do update
set nome = excluded.nome,
    perfil = excluded.perfil,
    cliente_id = excluded.cliente_id;

-- Confirme que as três contas foram encontradas e vinculadas.
select u.email, p.nome, p.perfil
from profiles p
join auth.users u on u.id = p.id
where lower(u.email) in ('mecanico@oficina.com', 'tecnico@fersa.com', 'admin@fersa.com')
order by u.email;
