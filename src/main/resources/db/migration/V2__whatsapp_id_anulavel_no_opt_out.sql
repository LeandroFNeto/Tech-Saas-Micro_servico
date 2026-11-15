alter table contatos alter column whatsapp_id drop not null;

update contatos set whatsapp_id = null, nome = null, final_telefone = null where nao_contatar = true;
