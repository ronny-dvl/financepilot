<div align="center">

<img src="./docs/images/logo.png" alt="Logo do FinancePilot" width="160" />

# FinancePilot

**Entenda para onde seu dinheiro está indo.**

Transforme extratos bancários em informações financeiras claras, organizadas e fáceis de explorar.

![Java](https://img.shields.io/badge/Java_21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![React](https://img.shields.io/badge/React-149ECA?style=for-the-badge&logo=react&logoColor=white)
![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?style=for-the-badge&logo=typescript&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)

> A demonstração completa no navegador ainda está em desenvolvimento.

[Sobre](#sobre-o-projeto) · [Funcionalidades](#funcionalidades) · [Demonstração](#demonstração) · [Tecnologias](#tecnologias) · [API](#documentação-da-api)

</div>

## Demonstração

### Landing page

<p align="center">
  <img src="./docs/images/landing-page.jpeg" alt="Landing page do FinancePilot" width="900" />
</p>

### Upload de extratos

<p align="center">
  <img src="./docs/images/upload-pdf.png" alt="Interface de upload de PDF do FinancePilot" width="900" />
</p>

> As imagens mostram telas do projeto. O processamento de PDFs está disponível na API; a integração completa com o frontend segue em desenvolvimento.

## Sobre o projeto

O **FinancePilot** é uma aplicação web de análise financeira pessoal. A proposta é permitir que o usuário envie um extrato bancário em PDF e visualize suas movimentações de forma mais intuitiva, com resumos, gráficos e análises de gastos.

O projeto está **em desenvolvimento**: o backend já conta com extração de texto de PDFs e processamento de transações de extratos Itaú e Santander; a experiência visual e as análises estão em evolução.

### Por que foi criado?

Extratos bancários contêm informações importantes, mas nem sempre são fáceis de interpretar. O FinancePilot nasceu para simplificar essa leitura e ajudar pessoas a entender seus hábitos financeiros, ao mesmo tempo em que explora desafios reais de desenvolvimento full-stack, processamento de arquivos e organização de dados.

## Funcionalidades

| Funcionalidade | Situação |
| --- | --- |
| Upload de extratos bancários em PDF pela API | Implementado |
| Extração de texto de PDFs com Apache PDFBox | Implementado |
| Identificação de transações em extratos Itaú e Santander | Implementado |
| Landing page responsiva | Em desenvolvimento |
| Resumo de entradas, saídas e saldo | Em desenvolvimento |
| Categorização automática de despesas | Planejado |
| Dashboard com gráficos e indicadores | Planejado |
| Análise de gastos por destinatário | Em desenvolvimento |
| Persistência de dados em PostgreSQL | Planejado |

### Bancos suportados

- Itaú
- Santander

> O processamento foi desenvolvido para os formatos de extrato já testados desses bancos. Outros layouts podem exigir adaptações.

## Tecnologias

**Backend:** Java 21, Spring Boot, Maven, Apache PDFBox, JUnit 5, Mockito e MockMvc.

**Frontend:** React, TypeScript, CSS e Framer Motion.

**Ferramentas:** Git, GitHub e Postman.

**Planejado:** PostgreSQL e integração mais completa entre frontend e backend.

## Acesso e uso

O FinancePilot foi pensado para ser usado diretamente no navegador, sem instalação pelo usuário final. A experiência completa de envio do extrato e visualização dos resultados ainda está em desenvolvimento.

Atualmente, os recursos de leitura e processamento de extratos Itaú e Santander podem ser testados pela API. Veja os endpoints abaixo.

<details>
<summary><strong>🛠️ Executar localmente</strong></summary>

### Pré-requisitos

- Git
- Java JDK 21
- Node.js e npm compatíveis com o frontend

### Clonar o repositório

```bash
git clone https://github.com/ronny-dvl/financepilot.git
cd financepilot
```

### Backend

No Windows (PowerShell):

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

No Linux/macOS:

```bash
cd backend
./mvnw spring-boot:run
```

A API geralmente fica disponível em `http://localhost:8080`, salvo alterações na configuração.

### Frontend

Em outro terminal, a partir da raiz do repositório:

```bash
cd frontend
npm install
npm run dev
```

Abra o endereço local informado pelo servidor de desenvolvimento. A integração completa entre frontend e backend ainda está em evolução.

</details>

## Documentação da API

### `POST /api/statements/upload`

Recebe um arquivo PDF por `multipart/form-data` e retorna informações sobre o texto extraído.

**Exemplo:**

```bash
curl -X POST http://localhost:8080/api/statements/upload \
  -F "file=@extrato.pdf"
```

**Resposta ilustrativa:**

```json
{
  "fileName": "extrato.pdf",
  "pages": 3,
  "characters": 4826,
  "text": "Texto extraído do extrato..."
}
```

### `POST /api/statements/transactions`

Recebe um extrato PDF por `multipart/form-data` e retorna as transações reconhecidas.

**Exemplo:**

```bash
curl -X POST http://localhost:8080/api/statements/transactions \
  -F "file=@extrato.pdf"
```

**Resposta ilustrativa:**

```json
[
  {
    "date": "2026-10-20",
    "description": "PIX TRANSF EXEMPLO",
    "amount": 75.90,
    "type": "EXPENSE"
  }
]
```

O campo `type` identifica movimentações como `INCOME` ou `EXPENSE`. As respostas acima são apenas exemplos; os resultados dependem do arquivo enviado.

## Roadmap

- [x] Estruturar backend com Spring Boot
- [x] Implementar upload e extração de PDF
- [x] Criar parser para transações Itaú
- [x] Adicionar suporte a extratos Santander
- [x] Adicionar testes automatizados ao processamento de PDFs
- [ ] Finalizar integração frontend/backend
- [ ] Implementar categorização de despesas
- [ ] Concluir resumos financeiros e dashboard
- [ ] Adicionar persistência com PostgreSQL
- [ ] Expandir suporte para outros bancos
- [ ] Publicar uma demonstração completa

## Contribuições

Sugestões, correções e melhorias são bem-vindas. Abra uma *issue* para discutir mudanças ou envie um *pull request* com uma descrição clara do que foi alterado.

## Licença

Distribuído sob a licença MIT. Consulte o arquivo [LICENSE](LICENSE).

---

<div align="center">

Desenvolvido por [Ronny](https://github.com/ronny-dvl).

</div>
