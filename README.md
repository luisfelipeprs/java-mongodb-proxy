# mongo-proxy

Aplicação Java 21 (Micronaut) que atua como **proxy do MongoDB**: mantém um
pool de conexões aberto e expõe endpoints HTTP simples para que a Lambda
principal não precise conectar diretamente no Mongo.

```
Lambda (aplicação principal) --HTTP--> mongo-proxy (EC2) --pool--> MongoDB
```

## Por que isso ajuda

Lambdas sofrem com o problema de abrir uma conexão nova (ou até estourar o
limite de conexões do Mongo) a cada invocação, já que cada execução pode
rodar em um container isolado. Mantendo um processo de longa duração na EC2
com um `MongoClient` único e um pool de conexões configurado, o Mongo recebe
um número controlado e estável de conexões, e a Lambda só faz uma chamada
HTTP simples.

## Endpoints

| Método | Caminho   | Descrição                          |
|--------|-----------|-------------------------------------|
| GET    | `/users`  | Lista os usuários da coleção Mongo  |
| GET    | `/health` | Health check (sempre livre de auth) |

## Build

Requer Java 21 e Maven.

```bash
mvn clean package
```

Isso gera um jar executável (com todas as dependências, via shade plugin) em
`target/mongo-proxy-0.1.jar`.

## Rodar localmente

```bash
export MONGODB_URI="mongodb://localhost:27017"
export MONGODB_DATABASE="mydb"
java -jar target/mongo-proxy-0.1.jar
```

Testar:

```bash
curl http://localhost:8080/users
curl http://localhost:8080/health
```

## Rodar com Docker

Requer apenas Docker instalado (não precisa de Maven/Java na máquina host;
o build da imagem já faz isso na etapa de build do `Dockerfile`).

```bash
export MONGODB_URI="mongodb://usuario:senha@seu-host-mongo:27017"
export MONGODB_DATABASE="mydb"
export PROXY_API_KEY="troque-por-uma-chave-secreta"

docker compose up -d --build
```

Testar:

```bash
curl http://localhost:8080/health
curl -H "X-API-Key: troque-por-uma-chave-secreta" http://localhost:8080/users
```

Ver logs / status:

```bash
docker compose logs -f
docker compose ps
```

Sem docker-compose, o equivalente em `docker run` puro seria:

```bash
docker build -t mongo-proxy .
docker run -d \
  --name mongo-proxy \
  --restart unless-stopped \
  -p 8080:8080 \
  -e MONGODB_URI="mongodb://usuario:senha@seu-host-mongo:27017" \
  -e MONGODB_DATABASE="mydb" \
  -e PROXY_API_KEY="troque-por-uma-chave-secreta" \
  mongo-proxy
```

## Deploy na EC2 (com Docker)

1. Instale Docker na instância (Amazon Linux 2023, por exemplo):
   ```bash
   sudo dnf install -y docker
   sudo systemctl enable --now docker
   sudo usermod -aG docker ec2-user   # evita precisar de sudo em todo comando docker
   ```
   Isso já resolve o "problema do systemd": você não cria unit file próprio
   para a aplicação; o Docker daemon (que já roda como serviço do sistema)
   cuida de subir o container no boot, e o `restart: unless-stopped` cuida de
   recuperar o container se ele cair.

2. Envie o código para a instância (via `git clone`, `scp` ou pipeline de CI/CD):
   ```bash
   scp -r mongo-proxy ec2-user@SEU_IP:/opt/
   ssh ec2-user@SEU_IP
   cd /opt/mongo-proxy
   ```

3. Suba o container:
   ```bash
   export MONGODB_URI="mongodb://usuario:senha@seu-host-mongo:27017"
   export MONGODB_DATABASE="mydb"
   export PROXY_API_KEY="troque-por-uma-chave-secreta"
   docker compose up -d --build
   ```

4. Configure o Security Group da EC2 para liberar a porta `8080` **apenas**
   para o Security Group/CIDR usado pela Lambda (idealmente, Lambda e EC2 na
   mesma VPC, sem expor a porta publicamente).

> Nota: o arquivo `deploy/mongo-proxy.service` (systemd) foi mantido no
> repositório apenas como referência/alternativa caso você prefira rodar o
> jar diretamente sem Docker; mas com Docker ele não é mais necessário.

### Indo além: registro de imagens e CI/CD

Para não depender de copiar código para a EC2 manualmente, o caminho comum é:

1. Buildar a imagem no CI (GitHub Actions, GitLab CI, etc.) e publicar no
   **Amazon ECR** (`docker push`).
2. Na EC2, trocar o `build: .` do `docker-compose.yml` por `image:
   SEU_ECR_URI/mongo-proxy:latest` e rodar `docker compose pull && docker
   compose up -d`.
3. Se a aplicação crescer, esse mesmo Dockerfile roda sem alterações em
   **ECS/Fargate** ou **Kubernetes (EKS)**; a EC2 solta de ser gerenciada na
   mão e passa a ser substituída por um orquestrador, que cuida de restart,
   scaling e rolling deploy automaticamente.

## Chamando o proxy a partir da Lambda

Exemplo em Node.js:

```javascript
const response = await fetch("http://IP_OU_DNS_INTERNO_DA_EC2:8080/users", {
  headers: { "X-API-Key": process.env.PROXY_API_KEY }
});
const users = await response.json();
```

Exemplo em Java:

```java
HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("http://IP_OU_DNS_INTERNO_DA_EC2:8080/users"))
        .header("X-API-Key", System.getenv("PROXY_API_KEY"))
        .GET()
        .build();
HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
```

## Observações / próximos passos

- O pool de conexões é gerenciado pelo próprio driver oficial do MongoDB
  (`mongodb-driver-sync`), configurado em `MongoConfig`. Não é necessário
  nenhum pool externo.
- Para produção, recomenda-se: colocar a EC2 dentro de uma Auto Scaling Group
  (ou pelo menos com restart automático, já coberto pelo `Restart=on-failure`
  do systemd), usar HTTPS (via um Application Load Balancer na frente) e
  manter Lambda + EC2 na mesma VPC/subnets privadas.
- Este projeto é intencionalmente minimalista: apenas o endpoint de listagem
  de usuários foi implementado. Para novos endpoints, basta seguir o mesmo
  padrão de `UserController` + `UserService`.
