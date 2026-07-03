# Colocar a aplicação na Internet

Este guia explica como alojar a aplicação para lhe aceder **de qualquer lugar**.
A aplicação já está preparada para isso (configuração por variáveis de ambiente,
`Dockerfile` incluído).

---

## ⚠️ Antes de mais — Segurança (obrigatório)

Ao ficar na internet, **qualquer pessoa** pode tentar entrar. Portanto:

1. **Mude a palavra-passe do administrador.** Faz-se de duas formas:
   - Na variável de ambiente `APP_ADMIN_PASSWORD` (definida no alojamento), **ou**
   - Depois de entrar, no menu **Utilizadores → editar admin → nova palavra-passe**.
2. A **base de dados** passa a ter uma palavra-passe forte (é o alojamento que a gera).
3. Use sempre **HTTPS** (as plataformas abaixo fornecem-no automaticamente).

---

## Opção recomendada (mais simples): Railway

O [Railway](https://railway.app) aloja a aplicação **e** a base de dados MySQL, com
HTTPS automático. Tem um plano inicial barato (uso pago ao consumo, tipicamente
poucos euros/mês para uma adega pequena).

### Passos

1. **Pôr o código no GitHub**
   - Cria conta em <https://github.com> (grátis).
   - Cria um repositório novo e envia esta pasta do projeto para lá.
     (Posso preparar-te isto — ver secção "Ajuda para o GitHub" no fim.)

2. **Criar o projeto no Railway**
   - Entra em <https://railway.app> com a conta GitHub.
   - **New Project → Deploy from GitHub repo** e escolhe o repositório.
   - O Railway deteta o `Dockerfile` e constrói a aplicação sozinho.

3. **Adicionar a base de dados**
   - No mesmo projeto: **New → Database → Add MySQL**.
   - O Railway cria a base e mostra as credenciais (host, porta, utilizador, senha, nome).

4. **Ligar a aplicação à base de dados (variáveis de ambiente)**
   No serviço da aplicação, separador **Variables**, adiciona:

   | Variável | Valor |
   |---|---|
   | `SPRING_PROFILES_ACTIVE` | `mysql` |
   | `DB_URL` | `jdbc:mysql://HOST:PORTA/railway?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Europe/Lisbon&allowPublicKeyRetrieval=true&useSSL=false` |
   | `DB_USER` | (o utilizador do MySQL do Railway) |
   | `DB_PASSWORD` | (a senha do MySQL do Railway) |
   | `APP_ADMIN_PASSWORD` | (uma palavra-passe forte à tua escolha) |

   > Substitui `HOST`, `PORTA` e o nome da base (`railway`) pelos valores que o
   > Railway te mostra na base de dados. O Railway também expõe estes valores em
   > variáveis próprias (ex.: `${{MySQL.MYSQL_URL}}`) que podes reutilizar.

5. **Publicar**
   - O Railway constrói e arranca. Em **Settings → Networking → Generate Domain**
     obténs um endereço público com HTTPS, tipo `https://a-tua-adega.up.railway.app`.
   - Abre esse endereço e entra com `admin` + a palavra-passe que definiste.

---

## Opção alternativa: Servidor próprio (VPS)

Mais controlo e custo fixo (~4–6 €/mês em Hetzner, Contabo, DigitalOcean…).
Resumo (requer conhecimentos de Linux):

1. Alugar um VPS com Ubuntu.
2. Instalar Java 17 e MySQL (`apt install openjdk-17-jre mysql-server`).
3. Criar a base `gestao_adegas` e um utilizador com senha forte.
4. Enviar o ficheiro `target/gestao-adegas-0.1.0.jar` para o servidor.
5. Correr com as variáveis de ambiente:
   ```bash
   export SPRING_PROFILES_ACTIVE=mysql
   export DB_URL="jdbc:mysql://localhost:3306/gestao_adegas?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Europe/Lisbon"
   export DB_USER=adega
   export DB_PASSWORD='senha-forte'
   export APP_ADMIN_PASSWORD='senha-admin-forte'
   java -jar gestao-adegas-0.1.0.jar
   ```
6. Colocar um **Nginx** à frente com **HTTPS** (Let's Encrypt) e um domínio.
7. Correr como serviço (systemd) para arrancar sozinho.

---

## Variáveis de ambiente (resumo)

| Variável | Para quê | Exemplo |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Usar MySQL | `mysql` |
| `DB_URL` | Endereço da base de dados | `jdbc:mysql://host:3306/nome?...` |
| `DB_USER` | Utilizador da base | `adega` |
| `DB_PASSWORD` | Senha da base | (forte) |
| `APP_ADMIN_PASSWORD` | Senha do admin da app | (forte) |
| `PORT` | Porta (as plataformas definem-na) | `8080` |

As tabelas são criadas automaticamente no primeiro arranque.

---

## Custos (ordem de grandeza)

- **Railway / Render / Fly.io:** planos ao consumo; adega pequena ~3–10 €/mês.
- **VPS (Hetzner/Contabo):** ~4–6 €/mês fixo.
- **Domínio próprio** (opcional): ~10 €/ano.

---

## Ajuda para o GitHub

Se não souberes pôr o código no GitHub, diz-me: eu inicializo o repositório
local (`git`) e deixo-te os comandos exatos para enviar (`git push`) — só precisas
de criar a conta e o repositório vazio no GitHub.
