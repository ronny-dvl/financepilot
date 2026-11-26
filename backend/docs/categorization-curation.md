# Categorização: revisão prática da fase 4.3

## Política atual

Priorizar cobertura pela atividade predominante do estabelecimento, mantendo
25 categorias, 1.257 regras e 1.659 aliases, incluindo descritores comerciais
locais e Casa e utilidades. IDs antigos foram mantidos nas migrações de categoria.

Foram removidos os includeAny obrigatórios de compra e todos os concludeAny
dos catálogos, inclusive quarentenas de marketplaces. A capacidade opcional
continua no motor e nos testes; não há exigência geral de produto/recorrência.
Também foram retirados vetos de produto para Apple/Twitch e postos.

Permanecem normalização única, indexação, imutabilidade, IDs originais,
consolidações, limites de palavras, prioridades por especificidade,
refinamentos semânticos e diagnóstico interno sem dados financeiros em logs.
O CategoryResolver mantém a política de não desempatar marcas independentes
apenas pelo tamanho ou pela prioridade.

Exemplos: UBER → Transporte; UBER EATS/IFOOD/PIX MADERO → Alimentação;
CARREFOUR → Supermercado; SAMSUNG → Tecnologia; APPLE.COM/BILL/NETFLIX →
Assinaturas; RENNER → Vestuário; FERRETTO → Educação; PETZ → Pets;
SABESP → Contas essenciais; MERCADO LIVRE → Compras.

Contextos ficam restritos às siglas ambíguas (VIVO/CLARO/TIM com TELEFONIA,
EDP/LIGHT com ENERGIA) e à indicação de pessoa física em Pix. O matcher
reconhece o estabelecimento sozinho, mas não usa um sobrenome dentro de um
favorecido pessoal sem contexto comercial: PIX ENVIADO CARLOS FERRETTO →
Transferências pessoais, com natureza TRANSFER. Referências numéricas e intermediadores não
desqualificam um estabelecimento. Evidências comerciais explícitas permitem
o contexto; não foi criada uma lista de nomes pessoais.

## Correções das falhas Maven reportadas

- CurationEvidenceRegistryTest: o registro perdeu as razões das 337 regras
  mantidas sem alteração na fase 4.3. Madero foi a primeira falha encontrada,
  não um problema do classificador. Os registros ativos agora têm justificativas
  e políticas atuais coerentes. A asserção de justificativa não vazia foi
  preservada e a consistência da política prática também é verificada.
- FinancialCategorizationIntegrationTest, casos 29–44: BigDecimal recebido
  como -30 tinha escala zero. A fixture imprimia -30 em vez de -30,00, e o
  parser Itaú corretamente não reconhecia uma transação monetária compatível.
  movement agora exige duas casas decimais com setScale(2), sem arredondamento
  silencioso. Parser e validação de PDF não foram relaxados.
- Expectativas mudaram para os padrões comerciais solicitados, com casos
  explícitos independentes dos JSONs. As verificações monetárias, de sinal,
  transferência, investimentos, estornos e reconciliação continuam.

As suítes semânticas, de conflitos, catálogo, serviço e integração foram
ajustadas. Recursos opcionais de condições continuam com seus testes próprios.

## Rastreabilidade e limites

O [registro estruturado](categorization-curation.json) conserva fontes,
aliases inferidos, pendências e histórico de consolidações/remoções.
runtimePolicy/decisionReasons representam a política atual; changes e
phase43Statistics representam o histórico da curadoria anterior.
Não houve nova verificação comercial nem promoção de aliases a observados.

As categorias são padrões de organização financeira, não identificação do
produto. Uma marca exatamente igual ao nome completo de uma pessoa pode
continuar ambígua sem mais contexto. Marcas independentes conflitantes e
descrições incompreensíveis continuam em OUTROS. Natureza financeira pode
exigir revisão mesmo quando a categoria comercial é conhecida.

Os totais financeiros foram preservados.
As transferências pessoais agora compõem categorizedExpenses e expensesByCategory
por solicitação do gráfico, mantendo natureza TRANSFER. transferOutflow segue
separado para contas próprias, sem duplicar a saída. A reconciliação permanece:
totalOutflow = categorizedExpenses + uncategorizedOutflow + transferOutflow;
categorizedExpenses = soma(expensesByCategory). Entradas não viram consumo;
investimentos e transferências próprias ficam fora dele; estornos positivos
não compensam automaticamente gastos brutos.

Não foram executados builds, testes ou comandos Git nesta revisão.

