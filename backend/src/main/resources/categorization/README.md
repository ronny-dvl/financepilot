# Categorização local — política prática

22 catálogos, 23 categorias (OUTROS é fallback), 1.210 regras e 1.493 aliases
normalizados distintos. Classificação determinística, indexada e imutável;
sem rede, persistência de descrições ou novas dependências.

## Categoria padrão

A atividade predominante do estabelecimento determina a categoria padrão.
Marcas reconhecíveis não exigem produto adquirido, recorrência comprovada,
COMPRA, RSCSS ou PIX QRS: UBER é Transporte, CARREFOUR é Supermercado,
SAMSUNG é Tecnologia, APPLE.COM/BILL e NETFLIX são Assinaturas, RENNER é
Vestuário e MERCADO LIVRE é Compras. São padrões práticos de organização,
não comprovação do item comprado ou da recorrência de uma cobrança.

Contexto permanece obrigatório apenas para aliases ambíguos:
VIVO/CLARO/TIM exigem TELEFONIA e EDP/LIGHT exigem ENERGIA.
Intermediadores isolados (Mercado Pago, PagSeguro, Stripe, PayPal) não
identificam uma categoria; um estabelecimento reconhecido junto deles pode.

Em PIX ENVIADO/RECEBIDO/TRANSF, um nome antes do alias ou CPF explícito,
sem contexto comercial, impedem usar um sobrenome como marca:
PIX ENVIADO CARLOS FERRETTO fica em revisão; FERRETTO, PIX ENVIADO FERRETTO
e PIX MADERO são reconhecidos. Sufixos de cidade/filial, referências numéricas
e intermediadores não são tratados como nomes. Indicadores comerciais ou de contas
próprias preservam o contexto da operação. Não há lista de nomes de pessoas.
Essa heurística não comprova identidade: uma pessoa com nome exatamente igual
a uma marca pode ser indistinguível de um estabelecimento no texto disponível.

## Conflitos e recursos preservados

1. Refinamentos declarados prevalecem no mesmo trecho: UBER EATS/TRIP/ONE,
   Duolingo Plus/Super, Casa Riachuelo, Netflix Games e Copel Telecom.
2. Evidência SPECIFIC prevalece sobre descritores GENERIC.
3. Marcas específicas independentes de categorias distintas continuam em
   revisão quando não há interpretação claramente preferível. Prioridade
   e tamanho do alias não inventam uma decisão entre empresas concorrentes.
4. Prioridade/modo/ID estável escolhem somente a regra diagnóstica quando as
   evidências concordam na categoria. Ordem de arquivos não desempata.
5. Exclusões semânticas úteis permanecem: iFood Benefícios e Uber Cash, por
   exemplo, não assumem finalidade de alimentação/transporte.

Catálogos v1/v2 são compatíveis; include é AND, includeAny é OR de grupos AND,
exclude veta elegibilidade sem apagar evidência e refines declara relações
semânticas. concludeAny permanece disponível e testado para uma necessidade
explícita futura; nenhum catálogo atual exige comprovar finalidade de produto
por esse campo. As quarentenas gerais da fase 4.3 foram removidas.

Aliases/condições são normalizados uma vez. Duplicatas, condições inválidas,
referências ausentes e ciclos são rejeitados. STARTS_WITH é condição ancorada;
PREFIX é explícito e não deve ser usado em marcas. O estacionamento usa formas
inteiras em WORD, preservando proteção contra correspondências parciais.

Os 14 IDs originais e as consolidações úteis foram preservados. Os três IDs
históricos alimentacao.supermercado/mercadinho/atacadao pertencem a SUPERMERCADO.

## Categoria e consumo

Categoria comercial não determina sinal, natureza financeira ou consumo.
Serviços financeiros preservam entradas fora do consumo, investimentos no
residual, transferências próprias separadas e gastos brutos por categoria.
Devoluções negativas permanecem reconciliadas sem dupla contabilização.

Fontes e status comerciais permanecem no
[registro de curadoria](../../../../docs/categorization-curation.json).
Identidade de empresa não prova alias observado no banco; pendências não
foram promovidas a verificações. O registro não é carregado em runtime.
Resumo das alterações e fixtures em
[categorization-curation.md](../../../../docs/categorization-curation.md).
