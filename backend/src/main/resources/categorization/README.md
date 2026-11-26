# Catálogos locais de categorização

Os JSONs legados usam a versão 1; a versão 2 acrescenta contextos OR e refinamentos.
O loader aceita ambas as versões. IDs identificam regras estáveis;
não devem ser renomeados ao acrescentar aliases ou mover uma regra de categoria.
Os três IDs `alimentacao.supermercado`, `alimentacao.mercadinho` e
`alimentacao.atacadao` continuam históricos no catálogo SUPERMERCADO.

## Critérios de curadoria

- Usar nomes comerciais reconhecíveis e expressões bancárias com significado
  explícito. Não inferir CNPJ, razão social, titularidade ou atividade por nome
  de pessoa. Não acrescentar sufixos societários presumidos.
- Normalização já resolve caixa, acentos e pontuação: não cadastrar essas
  variantes como aliases distintos. Grafias concatenadas são diferentes quando
  a separação das palavras muda.
- Mercado Pago, PagSeguro, Stripe, PayPal, adquirentes e bancos, isoladamente,
  não identificam a finalidade de uma compra. Um estabelecimento reconhecido
  presente na mesma descrição pode fornecer a evidência que falta.
- As 25 regras já protegidas por COMPRA (por exemplo Renner, Fleury e Perini)
  agora exigem COMPRA, RSCSS ou PIX QRS no início da descrição, com limite de
  palavras. PIX ENVIADO/RECEBIDO ou COMPRA em uma observação não satisfazem o
  contexto. Não é uma curadoria completa de sobrenomes. VIVO/CLARO/TIM isolados exigem TELEFONIA; EDP/LIGHT exigem
  ENERGIA. Sem esse contexto, manter revisão.
- Investimentos exigem operação/instrumento explícitos; transferência exige
  indicação de contas próprias. Nome de banco ou corretora não é evidência.
- IOF, IPTU, IPVA e DARF isolados usam EXACT; descrições mais longas exigem as
  frases bancárias cadastradas. INSS sozinho pode ser recebimento e fica fora.
- Amazon Marketplace, AWS, Prime, Apple Store e Apple.com/bill possuem regras
  separadas. Não cadastrar AMAZON, APPLE ou MICROSOFT isolados.

## Prioridades e condições

O matcher continua indexado pelo primeiro token e mantém todas as ocorrências
de aliases cujas inclusões foram satisfeitas, inclusive quando uma exclusão
veta a elegibilidade. Esses registros internos contêm offsets, não o extrato.

1. Um refinamento declarado e elegível suprime apenas a ocorrência do pai
   contida no mesmo trecho da descrição. Menções independentes continuam visíveis.
2. Uma correspondência literal completa pode refinar um PREFIX do mesmo lexema.
3. Evidência SPECIFIC contextualizada prevalece sobre descritores GENERIC.
4. Categorias concorrentes no nível restante retornam OUTROS/UNKNOWN/requiresReview,
   mesmo com prioridades, comprimentos ou modalidades diferentes. Exclusões não
   apagam marcas específicas concorrentes nem tornam uma regra vetada elegível.
5. Apenas quando as evidências restantes concordam na categoria, prioridade e
   modalidade selecionam o ID diagnóstico; empate usa o menor ID. Comprimento
   de marca não decide a categoria. A ordem de arquivos ou marcas não desempata.

- Regras legadas mantêm seus IDs e prioridades originais (90–200).
- alimentacao.99food e transporte.99-app passam a 100, alinhadas às formas legadas
  equivalentes. Os 14 IDs originais são preservados.
- Novas marcas e frases explícitas: SPECIFIC, prioridade 250.
- Uber Eats/One e serviços Prime/Music: 350; AWS: 400; contas próprias: 450.
- Descritores genéricos: GENERIC, prioridade 60–65.
- include continua AND. includeAny é OR de grupos AND não vazios, além do include.
  Qualquer exclude veta a elegibilidade, preservando a evidência para conflitos.
- STARTS_WITH, reservado às condições v2, exige início e limite de palavras;
  não equivale ao PREFIX de caracteres e não pode ser usado como modalidade de alias.
- refines lista IDs de regras-pai. O loader rejeita referências ausentes,
  duplicadas, autorreferências e ciclos; a transitividade é compilada uma vez.
- Uber exclui UBER EATS/UBEREATS, UBER ONE/UBERONE e UBER CASH/UBERCASH.
  99 de transporte mantém FOOD/99FOOD em ambas as grafias; iFood mantém BENEFICIOS.
- AWS e Marketplace não usam mais vetos cruzados: ambas as evidências, inclusive
  concatenadas, chegam ao resolvedor. Prime/Video/Music concorrem com Marketplace
  se houver dois identificadores independentes; PRIME ou MUSIC isolados não são marcas.
- PREFIX fica limitado ao descritor ESTACIONAMENT (estacionamento/estacionamentos).
  Nomes de marcas, pessoas e intermediadores não usam correspondência parcial.

## Alterações pontuais da fase 4.2

Sem novas regras: 1.217 regras, 1.493 aliases normalizados distintos. O único
alias acrescentado é UBER TRIP em transporte.ubertrip, junto de UBERTRIP.

Refinamentos declarados (o primeiro ID refina o segundo):

- alimentacao.uber-eats -> transporte.uber
- assinaturas.uber-one -> transporte.uber
- transporte.ubertrip -> transporte.uber
- assinaturas.duolingo-plus -> educacao.duolingo
- assinaturas.super-duolingo -> educacao.duolingo
- compras.casa-riachuelo -> vestuario.riachuelo
- games.netflix-games -> assinaturas.netflix
- internet-telefonia.copel-telecom -> contas-essenciais.copel

Regras com contextos alternativos, mantendo os aliases e as proteções existentes:

- alimentacao.madero, alimentacao.seu-justino
- beleza-cuidados-pessoais.granado, beleza-cuidados-pessoais.bruna-tavares-cosmeticos,
  beleza-cuidados-pessoais.oceane
- compras.morana
- saude.fleury, saude.richet
- supermercado.angeloni, supermercado.giassi, supermercado.zaffari,
  supermercado.hirota-food, supermercado.nagumo, supermercado.dalben,
  supermercado.tauste, supermercado.villefort, supermercado.bretas, supermercado.perini
- transporte.hertz
- vestuario.lupo, vestuario.schutz, vestuario.ferracini, vestuario.renner,
  vestuario.hering, vestuario.marisa

Outras alterações: prioridade de alimentacao.99food e transporte.99-app;
exclusões de transporte.uber, compras.amazon-marketplace e tecnologia.amazon-web-services.

Os testes antigos que preferiam a maior prioridade entre categorias independentes
passam a exigir revisão. Preferência por frase maior exige refines explícito.
O caso financeiro COMPRA DE ACOES AWS mantém natureza desconhecida, revisão e o
mesmo residual; somente a categoria passa de TECNOLOGIA para OUTROS por conflito.

## Limites

Classificação de categoria não confirma natureza, meio de pagamento ou despesa
de consumo. Esses serviços e contratos não são alterados pelos catálogos.
As descrições dos testes são sintéticas; não representam homologação de todos
os layouts bancários. Um nome comercial pode atuar em mais de um segmento;
retorno de catálogo não comprova identidade do recebedor. Descrições truncadas,
intermediadas sem estabelecimento ou ambíguas devem continuar em revisão.

Ao incluir regras, executar oportunamente a suíte, incluindo os parâmetros por
alias, os negativos, os conflitos e a validação estrutural. Não adicionar dados
de extratos reais ou dados pessoais aos catálogos/testes.
