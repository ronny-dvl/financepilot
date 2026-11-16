# Backend no Render e frontend na Vercel

O backend usa Docker multi-stage com JDK 21 e Maven Wrapper na compilação, JRE 21 na execução e usuário sem privilégios de root. Não há Dockerfile anterior a reaproveitar. O contexto é restrito aos fontes de produção e aos arquivos necessários ao Maven; PDFs, dados locais, testes e arquivos de ambiente ficam de fora.

## Configurar o Render

Crie um **Web Service**, conecte este repositório e selecione a branch que contém os arquivos de deploy.

| Campo | Valor |
| --- | --- |
| Language / Runtime | Docker |
| Instance Type | Free |
| Root Directory | `backend` |
| Dockerfile Path | `./Dockerfile` |
| Docker Build Context Directory | `.` |
| Health Check Path | `/api/health` |
| Docker Command | Deixar vazio; usar o ENTRYPOINT da imagem |
| Build / Start Command | Não configurar comandos adicionais; o Dockerfile cuida das duas etapas |

Os caminhos do Dockerfile e do contexto são relativos ao Root Directory.

Variáveis de ambiente no Render:

| Variável | Configuração |
| --- | --- |
| `CORS_ALLOWED_ORIGINS` | Obrigatória para este deploy: a origem HTTPS exata do frontend público, por exemplo `https://seu-projeto.vercel.app`. Sem `/` final, caminho, `*` ou aspas. |
| `PORT` | Usar o valor fornecido pelo Render; não precisa cadastrar manualmente. O fallback local é `8080`. |
| `JAVA_TOOL_OPTIONS` | Não precisa cadastrar: a imagem já define `-XX:MaxRAMPercentage=60.0 -XX:+ExitOnOutOfMemoryError -Djava.awt.headless=true`. Se sobrescrever, preserve uma margem para memória nativa/PDFBox. |

Se houver domínio próprio e domínio Vercel, liste ambos em `CORS_ALLOWED_ORIGINS`, separados por vírgula. Essa lista substitui os defaults locais; para usar também localhost contra o serviço publicado, adicione as origens locais explicitamente. URLs de Preview da Vercel não são liberadas automaticamente.

Sem a variável, o desenvolvimento local mantém `http://localhost:5173` e `http://localhost:5174`. A configuração rejeita curingas e URLs com caminhos. `POST`, `OPTIONS` e o envio de `FormData` multipart continuam habilitados; deixe o navegador gerar o Content-Type com o boundary.

## Configurar a Vercel

Cadastre **`VITE_API_URL`** no projeto do frontend:

```text
VITE_API_URL=https://seu-servico.onrender.com
```

Use a URL pública HTTPS real do Render, sem `/api` nem `/api/statements/analyze`. Selecione o ambiente correspondente (Production e, se necessário, Preview/Development) e faça um novo deploy do frontend: o Vite incorpora essa variável durante o build. Sem a variável, o cliente mantém `http://localhost:8080`. Não é um segredo.

## Health check após publicar

Abra `https://seu-servico.onrender.com/api/health` ou execute por sua conta:

```powershell
curl.exe -i https://seu-servico.onrender.com/api/health
```

Resultado esperado: HTTP `200`, com `{"status":"ok"}`. Esse endpoint confirma que a aplicação está respondendo; não processa PDFs nem valida layouts bancários. Abrir o health check diretamente não verifica CORS: a integração deve ser conferida pelo upload na origem pública autorizada.

## Limitações e cuidados

- O plano gratuito pode suspender o serviço após inatividade e levar cerca de um minuto para reiniciar. O frontend aguarda até **180 segundos**, mantém cancelamento e não repete uploads automaticamente. Após timeout/erro, o usuário pode tentar novamente.
- A JVM utiliza até 60% da memória detectada para heap; isso não limita o processo inteiro. PDFs grandes, complexos ou vários uploads simultâneos podem exceder os recursos da instância gratuita. As validações atuais e os limites de upload de 10 MB foram preservados.
- O limite de 10 MB da requisição inclui o envelope multipart; um PDF exatamente no limite pode ser rejeitado por causa desse overhead.
- Não configure disco persistente, logs de arquivos ou dumps de heap. A aplicação não salva extratos de forma persistente. O multipart pode usar arquivos temporários no armazenamento efêmero do container, removidos pelo servidor ao concluir a requisição.
- Os logs HTTP do Spring ficam em INFO para evitar a serialização de textos, nomes e transações. Não habilite DEBUG/TRACE de `org.springframework.web`, request logging de corpos ou ferramentas que capturem payloads. Os erros HTTP não incluem stack trace ou mensagens internas não tratadas.
- CORS controla o acesso pelo navegador; a API continua pública e sem autenticação. Não equivale a controle de acesso ou limitação de abuso. Para publicação aberta, considere os limites operacionais do plano gratuito e a exposição dos endpoints de processamento existentes.
- O build no Render precisa acessar o Maven Central e o registro das imagens Eclipse Temurin. As versões do projeto e dos parsers não foram alteradas.
- Nenhum build, teste, Docker ou validação de deploy foi executado nesta preparação.

Referências: [monorepos](https://render.com/docs/monorepo-support), [Docker](https://render.com/docs/docker), [health checks](https://render.com/docs/health-checks) e [limitações do plano gratuito](https://render.com/docs/free).
