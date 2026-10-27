-- =====================================================================
-- FERSA Care — Seed (dados fictícios, os mesmos do protótipo Web)
-- Rode DEPOIS de schema.sql.
--
-- Usuários de teste (mecânico/técnico/admin) NÃO estão aqui: o
-- Supabase Auth cuida de senha/hash, então crie-os pelo Dashboard
-- (Authentication → Add user) ou via API Admin, e só then insira a
-- linha correspondente em `profiles` — modelo no final deste arquivo.
-- =====================================================================

-- IDs fixos (facilita ler e religar as tabelas de junção neste arquivo)
-- FRS-1001..1005 = peças; f1..f6 = ferramentas; p1..p9 = problemas

-- ---------------------------------------------------------------------
-- Peças
-- ---------------------------------------------------------------------
insert into pecas (id, part_number, nome, categoria, imagem_descricao) values
  ('10000000-0000-0000-0000-000000000001', 'FRS-1001', 'Rolamento de Roda Dianteira', 'Rolamento de Cubo', 'Rolamento cônico de duas carreiras, cor aço fosco'),
  ('10000000-0000-0000-0000-000000000002', 'FRS-1002', 'Rolamento de Câmbio', 'Rolamento de Transmissão', 'Rolamento cilíndrico de esferas, com anel de vedação'),
  ('10000000-0000-0000-0000-000000000003', 'FRS-1003', 'Rolamento de Diferencial', 'Rolamento de Eixo', 'Rolamento cônico reforçado, com porca de fixação'),
  ('10000000-0000-0000-0000-000000000004', 'FRS-1004', 'Rolamento de Suspensão', 'Rolamento de Suspensão', 'Rolamento de agulhas com anel de retenção'),
  ('10000000-0000-0000-0000-000000000005', 'FRS-1005', 'Rolamento de Eixo Comando', 'Rolamento de Motor', 'Rolamento de esferas de precisão, baixo atrito');

-- ---------------------------------------------------------------------
-- Aplicações (veículo/modelo) + vínculo com peças
-- ---------------------------------------------------------------------
insert into aplicacoes (id, veiculo, modelo) values
  ('20000000-0000-0000-0000-000000000001', 'Volvo FH 540', 'FH 540 6x2'),
  ('20000000-0000-0000-0000-000000000002', 'Scania R450', 'R450 Highline'),
  ('20000000-0000-0000-0000-000000000003', 'Mercedes-Benz Actros', 'Actros 2651'),
  ('20000000-0000-0000-0000-000000000004', 'DAF XF', 'XF 480'),
  ('20000000-0000-0000-0000-000000000005', 'Iveco S-Way', 'S-Way 460');

insert into peca_aplicacoes (peca_id, aplicacao_id) values
  ('10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001'), -- FRS-1001 x Volvo FH 540
  ('10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002'), -- FRS-1001 x Scania R450
  ('10000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000003'), -- FRS-1002 x Mercedes Actros
  ('10000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000004'), -- FRS-1002 x DAF XF
  ('10000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000005'), -- FRS-1003 x Iveco S-Way
  ('10000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000002'), -- FRS-1003 x Scania R450
  ('10000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000001'), -- FRS-1004 x Volvo FH 540
  ('10000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000004'), -- FRS-1004 x DAF XF
  ('10000000-0000-0000-0000-000000000005', '20000000-0000-0000-0000-000000000003'), -- FRS-1005 x Mercedes Actros
  ('10000000-0000-0000-0000-000000000005', '20000000-0000-0000-0000-000000000005'); -- FRS-1005 x Iveco S-Way

-- ---------------------------------------------------------------------
-- Ferramentas + vínculo com peças
-- ---------------------------------------------------------------------
insert into ferramentas (id, nome, descricao, uso, procedimento) values
  ('30000000-0000-0000-0000-000000000001', 'Extrator de rolamento', 'Ferramenta usada para remover rolamentos prensados sem danificar o eixo ou o alojamento.', 'Remoção', 'Posicione as garras atrás da pista interna e acione o fuso lentamente até soltar o rolamento.'),
  ('30000000-0000-0000-0000-000000000002', 'Torquímetro', 'Aplica o torque exato especificado no manual, evitando aperto excessivo ou insuficiente.', 'Fixação', 'Ajuste o valor de torque especificado e aperte em sequência cruzada quando houver mais de um ponto.'),
  ('30000000-0000-0000-0000-000000000003', 'Prensa hidráulica', 'Utilizada para assentar rolamentos de forma uniforme, sem impacto.', 'Montagem', 'Apoie a peça corretamente e aplique força de forma gradual até o batente.'),
  ('30000000-0000-0000-0000-000000000004', 'Martelo de nylon', 'Usado para pequenos ajustes de montagem sem marcar a peça.', 'Montagem', 'Aplique golpes leves e uniformes ao redor do anel, nunca em um único ponto.'),
  ('30000000-0000-0000-0000-000000000005', 'Alicate de anel de retenção', 'Remove e instala anéis de retenção (anéis elásticos) sem deformá-los.', 'Remoção e montagem', 'Encaixe as pontas do alicate nos furos do anel e comprima até liberar o encaixe.'),
  ('30000000-0000-0000-0000-000000000006', 'Prensa manual de bancada', 'Alternativa à prensa hidráulica para peças menores.', 'Montagem', 'Centralize a peça na base e acione a alavanca de forma gradual.');

insert into peca_ferramentas (peca_id, ferramenta_id) values
  ('10000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001'),
  ('10000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000002'),
  ('10000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000003'),
  ('10000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000001'),
  ('10000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000004'),
  ('10000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000002'),
  ('10000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000003'),
  ('10000000-0000-0000-0000-000000000004', '30000000-0000-0000-0000-000000000005'),
  ('10000000-0000-0000-0000-000000000004', '30000000-0000-0000-0000-000000000001'),
  ('10000000-0000-0000-0000-000000000005', '30000000-0000-0000-0000-000000000006'),
  ('10000000-0000-0000-0000-000000000005', '30000000-0000-0000-0000-000000000005');

-- ---------------------------------------------------------------------
-- Lubrificação (1 linha por peça)
-- ---------------------------------------------------------------------
insert into lubrificacoes (peca_id, tipo, intervalo, quantidade, procedimento, cuidados, erros_comuns) values
  ('10000000-0000-0000-0000-000000000001', 'Graxa à base de lítio EP2', 'A cada 40.000 km', '18 g por rolamento',
   'Aplique a graxa preenchendo aproximadamente 2/3 do espaço interno das carreiras, evitando excesso na cavidade do cubo.',
   array['Não misturar graxas de bases químicas diferentes.', 'Evitar contaminação com água durante a lavagem do veículo.'],
   array['Excesso de graxa, causando superaquecimento.', 'Uso de graxa fora da especificação, reduzindo a vida útil.']),
  ('10000000-0000-0000-0000-000000000002', 'Óleo de transmissão SAE 75W-90', 'Conforme troca de óleo da transmissão', 'Imerso no óleo do câmbio',
   'O rolamento é lubrificado pelo próprio óleo do câmbio; não há aplicação manual de graxa.',
   array['Verificar sempre o nível de óleo após a troca.'],
   array['Nível de óleo abaixo do especificado.', 'Uso de óleo com viscosidade incorreta.']),
  ('10000000-0000-0000-0000-000000000003', 'Óleo para diferencial SAE 85W-140', 'A cada 80.000 km', 'Conforme nível do cárter',
   'Completar o óleo pelo bujão de nível até o transbordamento leve, sinal de nível correto.',
   array['Verificar vazamentos nos retentores antes de completar.'],
   array['Nível de óleo abaixo do especificado.', 'Folga incorreta entre coroa e pinhão.']),
  ('10000000-0000-0000-0000-000000000004', 'Graxa à base de lítio EP2', 'A cada 30.000 km', '12 g por rolamento',
   'Aplicar a graxa nas agulhas antes da montagem e reforçar pelo ponto de lubrificação após instalado.',
   array['Evitar excesso, que pode danificar a vedação.'],
   array['Alinhamento incorreto da suspensão.', 'Falta de graxa nas agulhas antes da montagem.']),
  ('10000000-0000-0000-0000-000000000005', 'Óleo do motor conforme especificação do fabricante', 'Conforme troca de óleo do motor', 'Imerso no óleo do motor',
   'Lubrificado pela circulação do óleo do motor; não há aplicação manual.',
   array['Usar somente óleo homologado para o motor.'],
   array['Uso de óleo fora da especificação.', 'Troca de óleo atrasada.']);

-- ---------------------------------------------------------------------
-- Passo a passo de instalação (tutorial)
-- ---------------------------------------------------------------------
insert into tutorial_passos (peca_id, ordem, titulo, texto) values
  ('10000000-0000-0000-0000-000000000001', 1, 'Limpeza do alojamento', 'Limpe o alojamento do cubo antes de posicionar o rolamento. Remova resíduos de graxa antiga e partículas metálicas.'),
  ('10000000-0000-0000-0000-000000000001', 2, 'Posicionamento', 'Posicione o rolamento perpendicular ao eixo, sem inclinação, alinhando as pistas interna e externa.'),
  ('10000000-0000-0000-0000-000000000001', 3, 'Prensagem', 'Utilize a prensa hidráulica para assentar o rolamento até o batente, aplicando força de forma uniforme e gradual.'),
  ('10000000-0000-0000-0000-000000000001', 4, 'Conferência final', 'Confirme o giro livre, sem folga ou ponto duro. Gire o cubo manualmente em ambos os sentidos.'),

  ('10000000-0000-0000-0000-000000000002', 1, 'Alinhamento do eixo', 'Verifique o alinhamento do eixo de entrada antes da montagem.'),
  ('10000000-0000-0000-0000-000000000002', 2, 'Aquecimento (se indicado)', 'Aqueça levemente o rolamento para facilitar o encaixe, se indicado no manual do fabricante.'),
  ('10000000-0000-0000-0000-000000000002', 3, 'Montagem', 'Posicione com o martelo de nylon, batendo de forma uniforme ao redor do anel.'),
  ('10000000-0000-0000-0000-000000000002', 4, 'Verificação da vedação', 'Cheque a vedação após a montagem, garantindo que não haja folga entre o anel e o alojamento.'),

  ('10000000-0000-0000-0000-000000000003', 1, 'Limpeza do alojamento', 'Remova resíduos de graxa antiga do alojamento do diferencial.'),
  ('10000000-0000-0000-0000-000000000003', 2, 'Posicionamento', 'Posicione o rolamento com auxílio da prensa, sem forçar lateralmente.'),
  ('10000000-0000-0000-0000-000000000003', 3, 'Torque da porca', 'Aplique o torque especificado na porca de fixação, seguindo a sequência do manual.'),
  ('10000000-0000-0000-0000-000000000003', 4, 'Folga do pinhão', 'Verifique a folga do pinhão após a montagem, com relógio comparador.'),

  ('10000000-0000-0000-0000-000000000004', 1, 'Remoção do anel antigo', 'Retire o anel de retenção antigo com o alicate apropriado.'),
  ('10000000-0000-0000-0000-000000000004', 2, 'Posicionamento', 'Posicione o novo rolamento alinhado ao braço de suspensão.'),
  ('10000000-0000-0000-0000-000000000004', 3, 'Novo anel de retenção', 'Instale o anel de retenção novo, confirmando o encaixe completo no rasgo.'),
  ('10000000-0000-0000-0000-000000000004', 4, 'Checagem de folga', 'Movimente o braço manualmente para checar folga residual.'),

  ('10000000-0000-0000-0000-000000000005', 1, 'Verificação do eixo comando', 'Inspecione o eixo comando quanto a riscos ou desgaste antes de instalar o rolamento.'),
  ('10000000-0000-0000-0000-000000000005', 2, 'Posicionamento', 'Posicione o rolamento com a prensa manual, apoiando somente o anel interno.'),
  ('10000000-0000-0000-0000-000000000005', 3, 'Fixação', 'Instale o anel de trava e confirme o assentamento completo.'),
  ('10000000-0000-0000-0000-000000000005', 4, 'Teste de giro', 'Gire o eixo manualmente, checando ausência de travamento ou ruído.');

-- ---------------------------------------------------------------------
-- Vídeos
-- ---------------------------------------------------------------------
insert into videos (peca_id, titulo, duracao_segundos, tipo) values
  ('10000000-0000-0000-0000-000000000001', 'Instalação do FRS-1001', 134, 'instalacao'),
  ('10000000-0000-0000-0000-000000000001', 'Verificação de folga após montagem', 65, 'instalacao'),
  ('10000000-0000-0000-0000-000000000002', 'Instalação do FRS-1002', 182, 'instalacao'),
  ('10000000-0000-0000-0000-000000000003', 'Instalação do FRS-1003', 167, 'instalacao'),
  ('10000000-0000-0000-0000-000000000004', 'Instalação do FRS-1004', 118, 'instalacao'),
  ('10000000-0000-0000-0000-000000000005', 'Instalação do FRS-1005', 150, 'instalacao');

-- ---------------------------------------------------------------------
-- Problemas comuns + vínculo com peças
-- ---------------------------------------------------------------------
insert into problemas (id, titulo, causas, verificar, solucao, quando_tecnico) values
  ('40000000-0000-0000-0000-000000000001', 'Aquecimento', 'Lubrificação insuficiente ou pré-carga incorreta no aperto do cubo.',
   array['Quantidade de graxa aplicada', 'Torque de aperto do cubo', 'Temperatura após rodagem curta'],
   'Verifique a quantidade de graxa e reaperte conforme o torque especificado no manual.',
   'Se o aquecimento persistir após reaperto e relubrificação corretos.'),
  ('40000000-0000-0000-0000-000000000002', 'Ruído no cubo de roda', 'Contaminação por sujeira durante a instalação ou desgaste das pistas.',
   array['Marcas visuais nas pistas', 'Folga axial do rolamento', 'Presença de partículas na graxa'],
   'Inspecione visualmente as pistas; se houver marcas, substitua o rolamento.',
   'Ruído metálico constante mesmo após substituição.'),
  ('40000000-0000-0000-0000-000000000003', 'Vibração', 'Desalinhamento do eixo ou folga excessiva de montagem.',
   array['Alinhamento do eixo de entrada', 'Folga axial recomendada', 'Fixação dos parafusos'],
   'Refaça o alinhamento e verifique a folga axial recomendada.',
   'Vibração que aumenta progressivamente com o uso.'),
  ('40000000-0000-0000-0000-000000000004', 'Desgaste irregular por óleo', 'Contaminação do óleo ou nível abaixo do recomendado.',
   array['Nível de óleo', 'Cor e viscosidade do óleo', 'Presença de limalha no bujão magnético'],
   'Troque o óleo e verifique o nível conforme especificação.',
   'Limalha metálica visível no óleo trocado.'),
  ('40000000-0000-0000-0000-000000000005', 'Ruído no diferencial', 'Folga incorreta entre coroa e pinhão.',
   array['Folga do pinhão com relógio comparador', 'Marcas de contato na coroa'],
   'Reajuste a folga seguindo a sequência de torque do manual.',
   'Ruído em aceleração e desaceleração ao mesmo tempo.'),
  ('40000000-0000-0000-0000-000000000006', 'Lubrificação inadequada', 'Nível de óleo abaixo do especificado.',
   array['Nível pelo bujão', 'Vazamento nos retentores'],
   'Complete o óleo até o nível correto e verifique vazamentos nos retentores.',
   'Vazamento constante mesmo após troca do retentor.'),
  ('40000000-0000-0000-0000-000000000007', 'Dificuldade de instalação', 'Anel de retenção antigo deformado ou alojamento com rebarbas.',
   array['Estado do anel de retenção', 'Rebarbas no alojamento'],
   'Substitua o anel de retenção e lixe rebarbas do alojamento antes de montar.',
   'Peça nova não assenta mesmo após os ajustes.'),
  ('40000000-0000-0000-0000-000000000008', 'Desgaste irregular na suspensão', 'Alinhamento incorreto da suspensão.',
   array['Geometria da suspensão', 'Desgaste de buchas próximas'],
   'Verifique a geometria da suspensão em bancada especializada.',
   'Desgaste reaparece em poucos milhares de km.'),
  ('40000000-0000-0000-0000-000000000009', 'Ruído no eixo comando', 'Montagem com apoio incorreto ou eixo riscado.',
   array['Estado do eixo comando', 'Pressão de óleo do motor'],
   'Inspecione o eixo quanto a riscos; se houver dano, o eixo também deve ser avaliado.',
   'Ruído acompanhado de queda de pressão de óleo.');

insert into peca_problemas (peca_id, problema_id) values
  ('10000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001'),
  ('10000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000002'),
  ('10000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000003'),
  ('10000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000004'),
  ('10000000-0000-0000-0000-000000000003', '40000000-0000-0000-0000-000000000005'),
  ('10000000-0000-0000-0000-000000000003', '40000000-0000-0000-0000-000000000006'),
  ('10000000-0000-0000-0000-000000000004', '40000000-0000-0000-0000-000000000007'),
  ('10000000-0000-0000-0000-000000000004', '40000000-0000-0000-0000-000000000008'),
  ('10000000-0000-0000-0000-000000000005', '40000000-0000-0000-0000-000000000009'),
  ('10000000-0000-0000-0000-000000000005', '40000000-0000-0000-0000-000000000007');

-- ---------------------------------------------------------------------
-- Documentos (manuais) — texto pronto para, na Fase 6/7, ser dividido
-- em chunks e receber embeddings. Por enquanto ficam PUBLICADO, sem
-- chunks (o pipeline de indexação ainda não existe).
-- ---------------------------------------------------------------------
insert into documentos (titulo, tipo, peca_id, origem, status, versao, conteudo_bruto) values
  ('Manual FRS-1001 — Rolamento de Roda Dianteira', 'MANUAL', '10000000-0000-0000-0000-000000000001', 'CMS', 'PUBLICADO', 'v2.1',
   'Visão geral: o FRS-1001 é um rolamento cônico de duas carreiras, projetado para cubos de roda dianteira de veículos pesados.' ||
   ' Instalação: limpe o alojamento do cubo; posicione o rolamento perpendicular ao eixo; use a prensa hidráulica até o batente; confirme o giro livre, sem folga ou ponto duro.' ||
   ' Lubrificação: graxa à base de lítio EP2, 18 g por rolamento, a cada 40.000 km.' ||
   ' Diagnóstico: aquecimento indica verificar graxa e pré-carga do aperto; ruído indica inspecionar as pistas quanto a marcas de desgaste.'),
  ('Manual FRS-1002 — Rolamento de Câmbio', 'MANUAL', '10000000-0000-0000-0000-000000000002', 'CMS', 'PUBLICADO', 'v1.4',
   'Visão geral: o FRS-1002 é lubrificado pelo próprio óleo da transmissão e exige atenção ao alinhamento do eixo de entrada.' ||
   ' Instalação: verifique o alinhamento do eixo de entrada; aqueça levemente o rolamento se indicado; posicione com martelo de nylon; cheque a vedação após a montagem.' ||
   ' Lubrificação: óleo de transmissão SAE 75W-90, conforme intervalo de troca da transmissão.' ||
   ' Diagnóstico: vibração indica desalinhamento ou folga excessiva; desgaste irregular indica contaminação ou nível baixo de óleo.'),
  ('Manual FRS-1003 — Rolamento de Diferencial', 'MANUAL', '10000000-0000-0000-0000-000000000003', 'CMS', 'PUBLICADO', 'v1.2',
   'Visão geral: o FRS-1003 exige controle rigoroso da folga entre coroa e pinhão para funcionamento silencioso.' ||
   ' Instalação: remova resíduos de graxa antiga; posicione com a prensa sem forçar lateralmente; aplique o torque especificado na porca; verifique a folga do pinhão.' ||
   ' Lubrificação: óleo para diferencial SAE 85W-140, a cada 80.000 km.' ||
   ' Diagnóstico: ruído indica folga incorreta entre coroa e pinhão; lubrificação inadequada indica nível de óleo baixo.'),
  ('Manual FRS-1004 — Rolamento de Suspensão', 'MANUAL', '10000000-0000-0000-0000-000000000004', 'CMS', 'PUBLICADO', 'v1.0',
   'Visão geral: o FRS-1004 utiliza anel de retenção, que deve sempre ser substituído na troca do rolamento.' ||
   ' Instalação: retire o anel de retenção antigo; posicione o novo rolamento alinhado; instale o anel de retenção novo; movimente o braço para checar folga.' ||
   ' Lubrificação: graxa à base de lítio EP2, 12 g por rolamento, a cada 30.000 km.' ||
   ' Diagnóstico: dificuldade de instalação indica anel deformado ou rebarbas no alojamento; desgaste irregular indica alinhamento incorreto da suspensão.'),
  ('Manual FRS-1005 — Rolamento de Eixo Comando', 'MANUAL', '10000000-0000-0000-0000-000000000005', 'CMS', 'PUBLICADO', 'v1.0',
   'Visão geral: o FRS-1005 é um rolamento de precisão; o apoio incorreto durante a prensagem é a principal causa de dano.' ||
   ' Instalação: inspecione o eixo comando quanto a riscos; apoie somente o anel interno ao prensar; instale o anel de trava; teste o giro manualmente.' ||
   ' Lubrificação: lubrificado pela circulação do óleo do motor; sem aplicação manual.' ||
   ' Diagnóstico: ruído indica montagem com apoio incorreto ou eixo riscado.');

-- Usuários de teste: crie as contas em Supabase Authentication e depois
-- execute database/test-users.sql para vincular os perfis pelo e-mail.
