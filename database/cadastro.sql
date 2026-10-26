-- =====================================================================
-- FERSA Care — Cadastro pela tela de login
-- Rode DEPOIS de schema.sql. Pode ser rodado mais de uma vez sem erro.
--
-- Quem se cadastra pela tela "Criar conta" é criado pelo Supabase Auth em
-- auth.users. Este trigger cria, na mesma hora, a linha em `profiles`
-- (que é onde o backend procura o perfil), SEMPRE como MECANICO — o que
-- vier no cadastro nunca escolhe o papel. Quem promove para TECNICO/ADMIN
-- é um administrador, no CMS (Usuários).
-- =====================================================================

create or replace function criar_profile_novo_usuario()
returns trigger
language plpgsql
security definer          -- roda com o dono da função: o Supabase Auth não tem permissão em public.profiles
set search_path = public
as $$
begin
  insert into profiles (id, nome, perfil)
  values (
    new.id,
    coalesce(nullif(trim(new.raw_user_meta_data ->> 'nome'), ''), split_part(new.email, '@', 1)),
    'MECANICO'
  )
  on conflict (id) do nothing;
  return new;
end;
$$;

drop trigger if exists trg_criar_profile_novo_usuario on auth.users;
create trigger trg_criar_profile_novo_usuario
  after insert on auth.users
  for each row execute function criar_profile_novo_usuario();
