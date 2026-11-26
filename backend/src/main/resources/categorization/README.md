# Catálogos locais de categorização

Cada JSON usa a versão 1 do contrato existente. IDs identificam regras estáveis;
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
- Marcas que também podem ser sobrenomes (por exemplo Renner, Fleury e Perini)
  exigem o token COMPRA. Isso favorece precisão e pode deixar PIX legítimos
  sem classificação. VIVO/CLARO/TIM isolados exigem TELEFONIA; EDP/LIGHT exigem
  ENERGIA. Sem esse contexto, manter revisão.
- Investimentos exigem operação/instrumento explícitos; transferência exige
  indicação de contas próprias. Nome de banco ou corretora não é evidência.
- IOF, IPTU, IPVA e DARF isolados usam EXACT; descrições mais longas exigem as
  frases bancárias cadastradas. INSS sozinho pode ser recebimento e fica fora.
- Amazon Marketplace, AWS, Prime, Apple Store e Apple.com/bill possuem regras
  separadas. Não cadastrar AMAZON, APPLE ou MICROSOFT isolados.

## Prioridades e condições

O resolvedor continua priorizando SPECIFIC sobre GENERIC; em seguida aplica
prioridade, modalidade, quantidade de tokens e comprimento. Um empate entre
categorias mantém OUTROS/UNKNOWN/requiresReview. A ordem dos arquivos não decide.

- Regras legadas mantêm seus IDs e prioridades originais (90–200).
- Novas marcas e frases explícitas: SPECIFIC, prioridade 250.
- Uber Eats/One e serviços Prime/Music: 350; AWS: 400; contas próprias: 450.
- Descritores genéricos: GENERIC, prioridade 60–65.
- Todas as inclusões são obrigatórias; qualquer exclusão veta a regra.
  Uber exclui EATS/ONE/CASH; 99 de transporte exclui FOOD/99FOOD; iFood exclui
  BENEFICIOS. AWS e Marketplace se excluem quando a descrição mistura ambos.
- PREFIX fica limitado ao descritor ESTACIONAMENT (estacionamento/estacionamentos).
  Nomes de marcas, pessoas e intermediadores não usam correspondência parcial.

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
