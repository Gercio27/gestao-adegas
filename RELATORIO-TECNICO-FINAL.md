# Relatório Técnico Final — Software de Gestão de Pequenas Adegas
### ACV – Produção e Comércio de Vinhos de Talha, Lda.

> Documento de apoio ao relatório académico. Reúne o contexto, a cronologia, a
> metodologia, os resultados e as decisões técnicas do desenvolvimento da
> aplicação. Estado do projeto: **concluído e em produção (online)**.

---

## 1. CONTEXTO E OBJETIVO DO PROJETO

### 1.1 Entidade
- **ACV – Produção e Comércio de Vinhos de Talha, Lda.**
- Sede: Rua Frei Amador Arrais, nº 9, loja 4, 7800-491 **Beja**.
- Adega: Praça 25 de Abril, 12, 7960-421 **Vila de Frades**.
- Contribuinte: **513 442 340**.
- Setor: produção e comércio de **vinhos de talha** (pequena adega).

### 1.2 Como o projeto foi criado (origem e método de trabalho)
O projeto nasceu de um **caderno de requisitos** entregue pelo chefe de estágio,
**Alexandre Frade**, datado de **7 de junho de 2026**. Esse documento — a "base"
do trabalho — definia, em forma de tabelas e fases:

1. Os **produtos, equipamentos e utensílios que entram nos processos** e não são
   gerados pela produção (pontos 1.1 a 1.25: uvas/castas, vinhas, trabalhadores,
   fornecedores de outsourcing, vasilame, transporte, adegas, equipamentos de
   descarga/moagem, talhas, cubas/depósitos, equipamentos de análise, arrefecimento,
   garrafas de Oligal, depósitos de passagem a limpo, máquinas de engarrafar/rolhar/
   rotular/encapsular, rolhas, garrafas/bag-in-box/garrafão, rótulos, cápsulas,
   caixas, etiquetas).
2. Os **produtos que resultam do processo produtivo** (pontos 2.1–2.5: mosto em
   fermentação, mosto atestado, vinho pronto a granel, vinho pronto engarrafado).
3. Os **produtos de terceiros** (pontos 3.1–3.5: vinho de terceiros a granel/
   engarrafado, rótulos, cápsulas, caixas e etiquetas de terceiros).
4. Os **processos por ordem cronológica, em 8 fases**, cada uma com os seus
   subprocessos.

A partir desta especificação, **a aplicação foi integralmente implementada por mim**
(estágio curricular), traduzindo cada tabela em fichas/entidades e cada fase/
subprocesso em processos de software.

### 1.3 Princípio geral de conceção (do caderno de requisitos)
> **"Registo logo quando acontece. Uma só vez, toda a informação."**

Traduzido em regras de implementação:
- Cada ficha/registo tem um **código automático** gerado pelo sistema (por prefixo).
- A informação é registada **no momento do evento** e **reaproveitada** pelas fases
  seguintes, sem reintrodução manual (ex.: a casta e a origem da uva propagam-se do
  planeamento até ao produto acabado).
- Ciclo coberto: **"da vinha até à rotulagem — produto acabado".**

### 1.4 Objetivo
Informatizar todo o ciclo produtivo da adega, das 8 fases cronológicas, garantindo
**rastreabilidade** (da uva ao engarrafado), **controlo de existências**
(talhas/depósitos cheios e vazios, com controlo de capacidade) e a emissão dos
**documentos obrigatórios** (DA — Documento de Acompanhamento IVV/CVR, Nota de
Entrega, etiquetas para talhas/depósitos/contentores exigidas pela CVRA/IVV).

### 1.5 Datas-chave
| Marco | Data |
|---|---|
| Entrega do caderno de requisitos (Alexandre Frade) — **início** | **07-06-2026** |
| **Conclusão** da aplicação (versão completa das 8 fases) | **02-07-2026** |
| Colocação em Git e **entrada em produção (online)** | 03-07-2026 |
| Afinações e redesenho do fluxo interligado (pós-conclusão) | 08-07 a 10-07-2026 |

---

## 2. AS 8 FASES — REQUISITO vs. IMPLEMENTAÇÃO

Estrutura implementada no enum `Fase` (`pt.acv.adega.processos.Fase`):

| Fase | Designação |
|---|---|
| 1 | Análise à maturação da uva e planeamento dos vinhos |
| 2 | Vindima |
| 3 | Moer a uva, encher as talhas/cubas de fermentação |
| 4 | Acompanhamento do processo de fermentação |
| 5 | O vinho está considerado pronto / estágio |
| 6 | Engarrafamento / enrolhamento |
| 7 | Rotulagem / embalamento |
| 8 | Passagem ao setor comercial |

**Padrão comum a todos os processos** (do princípio "quem fez, datas, meios,
métodos"): cada operação abre e preenche uma **ficha de processo** com estado
**ABERTO/FECHADO**, responsável, data/hora de início e fim, meios e métodos
utilizados. Ao **fechar**, o processo aplica os seus efeitos de forma transacional;
o **ADMIN** pode **reabrir** (revertendo os efeitos).

### Fase 1 — Análise à maturação e planeamento
- **1.1 Análise à maturação:** ficha com quem fez, datas, meios/métodos (refratómetro,
  prova, análises laboratoriais), vinha e casta e resultados (boletim de análise).
  Implementado com **Grau provável** e Massa Volúmica; apenas o grau provável é
  obrigatório.
- **1.2 Planeamento dos vinhos:** mapa por vinho/vinha que **absorve as análises de
  maturação** (dados automáticos não editáveis) e permite indicar, por casta, a
  **quantidade prevista a colher**, a **data prevista de vindima**, a **adega**, o
  **vinho** a produzir e a **% de participação** dessas uvas no vinho. Implementado
  como planeamento **por vinho** com parcelas e **saldo** (a produção prevista vive na
  parcela — `Parcela.producaoPrevistaKg` — e desce a cada vinho). Acesso restrito a
  ADMIN.

### Fase 2 — Vindima
- **2.1 Vindimar**, **2.2 Intervenções na uva no campo**, **2.3 Entregar na adega**
  (com registo da **origem**: própria — automática da vinha vindimada — ou de
  terceiros, com identificação do terceiro e do transporte), **2.4 Intervenções para
  conservação da uva na adega**. Implementado como **folha única** sobre o
  planeamento, com registo **por parcela/colheita** e **código único por colheita**
  (prefixo **VDM**). A vindima **acumula** colheitas.

### Fase 3 — Moagem e enchimento
- **3.1 Moer/encher talhas/cubas** (preenche automaticamente as fichas dos **mostos**
  resultantes), **3.2 Análise dos mostos** (ficha por talha/cuba), **3.3 Limpeza e
  tratamento de resíduos/massas/engaços**. Implementado como fluxo
  **adega → vinho → vindimas → enchimentos**, mostrando a adega, os **Kg realmente
  moídos** e a **sobra por moer**, com **litros de mosto = Kg × 60%**.

### Fase 4 — Acompanhamento da fermentação
- **4.1 Remontagens** (possibilidade de um registo para várias talhas de uma vez).
- **4.2 Atestos** — com **controlo automático de capacidade**: a talha que recebe não
  aceita mais do que a sua capacidade e a que dá não cede mais do que tinha.
- **4.3 Fim da fermentação alcoólica / passagem à malolática** e **4.4 Outras
  intervenções** (incluindo análises de vinho).
- **4.5 "O vinho está pronto"** — passagem de mosto a **vinho a granel** (um registo
  por talha ou vários), alimentando o **mapa de vinhos prontos a granel**.
- **4.6 Elaboração e registo dos lotes.**
- **4.7 Entradas de mosto de origem externa** e **4.8 Saída/venda de mostos** — com
  **DA (Documento de Acompanhamento obrigatório IVV/CVR)**, identificação da
  contraparte, meio de transporte e depósito de armazenamento, com **baixa** nos
  depósitos.
- **Regra transversal:** controlo permanente de **talhas/depósitos cheios e vazios**
  (não é possível registar mosto/vinho superior à capacidade dos depósitos
  registados); cada equipamento/depósito indica se é **próprio ou de terceiros**.

### Fase 5 — O vinho está pronto / estágio
- **5.1 Passagem a limpo / transfegas** para talhas/depósitos de estágio (soma ao mapa
  de existências de vinhos prontos a granel).
- **5.2 Acompanhamento do estágio** (com análises).
- **5.3 Entradas** e **5.4 Saídas** de vinho a granel de origem externa (com DA e
  emissão de **etiqueta** para colar na talha/depósito — exigência da CVRA).
- **5.5 Pedidos de certificação dos vinhos a granel** — na ficha de cada vinho por
  lote fica registado o pedido e o resultado das análises.

### Fase 6 — Engarrafamento / enrolhamento
- **6.1 Planeamento** dos vinhos a comercializar (nome do vinho; uso total ou parcial
  do vinho a granel do depósito; garrafas, rolhas e contentores, com aviso de stock).
- **6.2 Preparação de máquinas/garrafas/rolhas.**
- **6.3 Engarrafar/rolhar** — ao fechar, dá **baixa automática** no vinho a granel e
  nas rolhas/garrafas usadas; define lote e contentores; emite **etiqueta para o
  contentor** (exigência IVV/CVR).
- **6.4 Pedidos de certificação de vinhos engarrafados** — o contentor passa a ficar
  **certificado**, com **data de validade da certificação** e aviso para emitir nova
  etiqueta.

### Fase 7 — Rotulagem / embalamento
- **7.1 Preparação de garrafas/bag-in-box/garrafões** (regista a quantidade preparada,
  identificando o contentor), **7.2 Rotulagem** (garrafas e rótulos utilizados),
  **7.3 Embalamento** (cria ficha de produto acabado).

### Fase 8 — Passagem ao setor comercial
- Cria a ficha de vinhos em produtos acabados e emite **Nota de Entrega (NE)**.

---

## 3. METODOLOGIA E FERRAMENTAS

### 3.1 Metodologia
- Desenvolvimento **orientado ao caderno de requisitos**: cada tabela de
  produtos/equipamentos deu origem a uma **ficha/entidade**; cada fase/subprocesso deu
  origem a um **processo** de software com o ciclo abrir → editar → fechar → reabrir.
- **Registo único no momento do evento** e propagação automática da informação entre
  fases (rastreabilidade da uva ao engarrafado).
- Desenvolvimento **incremental** com **integração/entrega contínua**: cada afinação é
  versionada em Git e implantada automaticamente.

### 3.2 Arquitetura
- **Arquitetura MVC em camadas:** Controller → Service (`@Transactional` nas regras
  críticas) → Repository (Spring Data) → Base de Dados.
- Vistas **Thymeleaf** com **fragmentos** partilhados (`fragments/layout.html`:
  cabeçalho, barra de navegação, mensagens, scripts).
- **`common/BaseEntity`** (id, código único, datas) herdada por fichas, produtos e
  processos; **`CodigoService`** gera o código por prefixo, com bloqueio.
- **`processos/Processo`** (`@MappedSuperclass`): estado, responsável, datas,
  meios/métodos, autor, data de fecho.
- **`RecipienteService`** resolve referências de recipiente (`TALHA:id` /
  `DEPOSITO:id`) de forma uniforme.

### 3.3 Tecnologias
| Camada | Tecnologia |
|---|---|
| Linguagem | **Java 17** |
| Framework | **Spring Boot 3.3.4** (Web MVC, Data JPA/Hibernate, Security, Validation) |
| Vistas | **Thymeleaf** + **Bootstrap 5** + **Bootstrap Icons** (webjars locais) |
| Base de dados | **MySQL/MariaDB** (produção); **H2** (perfil `dev`, demonstração) |
| Build | **Maven** (artefacto `gestao-adegas` v`0.1.0`, jar `gestao-adegas-0.1.0.jar`) |
| Contentorização | **Docker** (Dockerfile multi-stage) |
| Repositório | **GitHub** — https://github.com/Gercio27/gestao-adegas (branch `main`) |
| Hospedagem | **Railway** — https://gestao-adegas-production.up.railway.app/ |

### 3.4 Justificação das escolhas
- **Java/Spring Boot**: robustez, transações (`@Transactional`) essenciais ao controlo
  de existências, e maturidade do ecossistema para uma aplicação de gestão.
- **Thymeleaf + Bootstrap**: renderização no servidor, simples de manter, sem
  necessidade de uma SPA.
- **MySQL**: relacional, adequado à rastreabilidade e integridade referencial.
- **Railway** foi escolhido para a colocação online por oferecer **MySQL num clique** e
  **deploy automático** por integração com o GitHub. Foram **descartadas** as opções
  **Vercel** (apenas JS/serverless, não corre Java) e **cPanel/alojamento partilhado**
  (apenas PHP). Alternativas identificadas para o futuro: **Fly.io** ou **VPS**.

### 3.5 Configuração e segurança
- Configuração por **variáveis de ambiente** com valores por omissão locais
  (`PORT:8080`, `DB_URL/DB_USER/DB_PASSWORD`, `APP_ADMIN_PASSWORD`).
- **Spring Security** com dois perfis: **ADMIN** (acesso total, pode reabrir/anular) e
  **OPERADOR** (vê apenas os processos que abriu).
- `ddl-auto=update` (o esquema é criado/atualizado automaticamente sem apagar dados).

---

## 4. RESULTADOS E DADOS CONCRETOS

> Nota: sendo uma aplicação de gestão (e não um estudo de dados), os "resultados"
> concretos são de dimensão de software, cobertura funcional e regras de domínio — não
> existem KPIs estatísticos.

### 4.1 Cobertura funcional
- **8 fases** do ciclo produtivo, com todos os subprocessos, **implementadas e em
  produção**.
- **Fichas** (produtos/equipamentos que entram): Castas, Vinhas (+Parcelas),
  Trabalhadores, Fornecedores, Adegas, Talhas, Depósitos, Consumíveis (garrafas,
  rolhas, rótulos, cápsulas, caixas, etiquetas), Utilizadores.
- **Produtos resultantes**: Mosto (em fermentação, atestado, vinho a granel), Vinho
  Engarrafado, Lotes.
- **Documentos imprimíveis**: Nota de Entrega, DA e Etiquetas (talha/depósito/
  engarrafado), com impressão via `@media print`.
- **Painel/Dashboard**: existências, ocupação de talhas/depósitos, alertas de stock
  baixo, processos abertos e fluxo das 8 fases.

### 4.2 Prefixos de código (rastreabilidade)
Fichas: Casta **CAS**, Vinha **VIN**, Trabalhador **TRB**, Adega **ADG**, Talha
**TLH**, Depósito **DEP**, Consumíveis **GAR/ROL/ROT/CAP/CAI/ETQ**.
Produtos: Mosto **MST**, Vinho Engarrafado **VEG**, Lote **LOT**.
Processos: Maturação **AMT**, Planeamento **PLN**, Vindima **VDM**, Moagem **MOA**,
Remontagem **REM**, Atesto **ATE**, Movimento de mosto **MOV**, Passagem **PVG**,
Certificação **CER**, Engarrafamento **ENG**, Rotulagem **RTL**, Comercial **PCO**.
Documento: **DA** (Documento de Acompanhamento).

### 4.3 Regras quantitativas de domínio
- **Litros de mosto previstos = Kg de uva × 60%** (rendimento assumido).
- Estados do mosto: **EM_FERMENTACAO → ATESTADO → VINHO_GRANEL**.
- **Controlo de capacidade** no atesto e nas entradas de mosto: nunca se excede a
  capacidade da talha/depósito de destino.
- Na passagem a limpo (4.5/5.1), o sistema reporta os **litros por talha/depósito** e o
  **total** convertido a vinho a granel.

### 4.4 Dimensão do código (estado atual)
- **134** ficheiros `.java`.
- **67** *templates* Thymeleaf (`.html`).
- **24** commits versionados no GitHub.

### 4.5 Estado de produção
A aplicação está **online e funcional** em
https://gestao-adegas-production.up.railway.app/ (base de dados MySQL com persistência
em volume; cada novo *deploy* atualiza a aplicação **sem apagar os dados**). O acesso
inicial faz-se com um utilizador **admin** (senha já alterada em produção).

---

## 5. DESAFIOS E LIMITAÇÕES

- **Controlo de existências e capacidade:** garantir que nunca se regista mosto/vinho
  acima da capacidade dos depósitos exigiu lógica transacional cuidadosa nos atestos e
  movimentos.
- **Rastreabilidade completa:** propagar a origem da uva (própria/terceiros) e a casta
  ao longo de 8 fases, sem reintrodução manual, foi um dos pontos mais exigentes.
- **Configuração de *deploy* (Railway):** o passo de pré-descarga de dependências Maven
  no Docker (`dependency:go-offline`) é pesado em memória e o *builder* por vezes
  interrompe-o — é **transitório**, resolvendo-se com um novo *push*. Não é erro de
  código.
- **Correção de `.gitignore`:** uma regra `java/` chegou a excluir também
  `src/main/java` (todo o código-fonte), o que fazia falhar o *build* no Railway
  ("Unable to find main class"); foi corrigida para `/java/`.
- **Limitações de escopo assumidas:** a "ligação direta aos sistemas do IVV/CVR"
  (referida no caderno como interrogação — "Ligação com os sistemas do IVV/CVRs?") não
  é uma integração automática; a certificação é registada manualmente (pedido,
  entidade, resultado e validade), que era o âmbito viável do estágio.

---

## 6. DECISÕES E ITERAÇÕES (pós-conclusão)

Depois da versão completa (02-07-2026), houve um **redesenho para interligar as fases**
(fluxo guiado), com estas iterações principais:

- **Planeamento** passou de dados soltos para **planeamento por vinho com saldo na
  parcela** (a produção prevista vive na parcela e desce a cada vinho).
- **Vindima** passou de "processo por evento" para **folha única** sobre o planeamento,
  acumulando colheitas com código VDM próprio.
- **Moagem** teve duas versões: (1.ª) folha por linha vindimada; (2.ª, adotada) fluxo
  **por adega → vinho → vindimas → enchimentos**, mostrando Kg moídos e sobra.
- **Fase 4 (fermentação)** foi renumerada 4.1–4.4 e os processos passaram a ser
  **guiados pela adega**:
  - **4.3 Movimento de mosto:** guiado por **adega + vinho**; a **casta deixou de ser
    pedida** (vem automaticamente do vinho selecionado).
  - **4.4 Passagem a limpo:** guiada por **adega**, listando os mostos em fermentação
    dessa adega e **reportando os litros por recipiente e o total** convertido a vinho
    a granel.
- **Certificação (Fase 5):** redesenhada para ser **por depósito**. Passou a permitir
  escolher **adega + vinho**, listar os **depósitos ainda não certificados**, marcar
  **um ou mais** para certificar e indicar **qual serviu de amostra ao CVR/IVV**. Para
  o **vinho engarrafado**, passou a buscar os **contentores de garrafas** (lotes
  engarrafados) e **não** as talhas — pelo que essa certificação só se faz **após o
  engarrafamento (Fase 6)**.

Elementos do modelo antigo "por evento" (ex.: processo de vindima por evento) ficaram
**órfãos** após o redesenho e são candidatos a limpeza futura.

---

## 7. PRÓXIMOS PASSOS / TRABALHO FUTURO

- **Limpeza de código legado** do modelo antigo por evento (vindima/moagem por evento,
  colunas órfãs).
- **Migração de hospedagem** para uma solução paga e robusta (VPS, ex.: Hostinger),
  após o período de testes, com exportação/importação do MySQL.
- **Integração com IVV/CVR** (se e quando as APIs estiverem disponíveis), para
  automatizar os pedidos de certificação.
- **Reforço de segurança em produção** (HTTPS, gestão de senhas, cópias de segurança
  regulares).
- **Restrição opcional** do filtro de certificação a granel para mostrar apenas
  depósitos (excluindo talhas), conforme preferência de utilização.

---

## ANEXO A — Caderno de requisitos (resumo estruturado, tal como recebido)

**Autor:** Alexandre Frade · **Data:** 7-06-2026 · **Princípio geral:** "Registo logo
quando acontece. Uma só vez, toda a informação."

**1. Produtos/equipamentos que entram (não gerados pela produção):** 1.1 Uvas (lista
nacional de castas por ordem alfabética, ficha única com código automático); 1.2
Vinhas (nome, localização, parcelas, casta e área por parcela, resumo por casta e área
total); 1.3 Trabalhadores; 1.4 Fornecedores de outsourcing; 1.5 Vasilame (baldes,
contentores, reboque); 1.6 Meios de transporte (camião, trator); 1.7 Adegas; 1.8
Equipamentos de descarga; 1.9 Equipamentos de moagem; 1.10 Talhas; 1.11
Cubas/Depósitos; 1.12 Equipamentos de análise e medida; 1.13 Equipamentos de
arrefecimento das talhas; 1.14 Garrafas de Oligal (azoto alimentar); 1.15 Depósitos
para a passagem a limpo; 1.16 Máquinas de engarrafar; 1.17 Máquinas de rolhar; 1.18
Rolhas; 1.19 Máquinas de rotular; 1.20 Máquinas de encapsular; 1.21
Garrafas/bag-in-box/garrafão; 1.22 Rótulos; 1.23 Cápsulas; 1.24 Caixas; 1.25
Etiquetas.

**2. Produtos resultantes da produção:** 2.1 Mosto em fermentação; 2.2 Mosto em
fermentação já atestado; 2.4 Vinho pronto a granel; 2.5 Vinho pronto engarrafado.

**3. Produtos de terceiros:** 3.1 Vinho de terceiros a granel em depósitos próprios;
3.2 Vinho de terceiros a granel em depósitos dos terceiros; 3.3 Vinho de terceiros em
garrafas/bag-in-box/garrafão; 3.4 Rótulos de terceiros; 3.5 Cápsulas de terceiros;
caixas e etiquetas de terceiros.

**Processos por fases:** ver secção 2 deste relatório (Fases 1 a 8 com todos os
subprocessos).

---

## ANEXO B — Referências bibliográficas

*Documentação do versionamento (GitHub), da hospedagem (Railway) e da integração
GitHub → Railway usada para o deploy automático. Consultadas em 13 de julho de 2026.*

1. GitHub, Inc. *GitHub Docs.* Disponível em: https://docs.github.com/
2. Railway. *Railway Documentation.* Disponível em: https://docs.railway.com/
3. Railway. *Deploy from a GitHub repository (GitHub Autodeploys).* Disponível em:
   https://docs.railway.com/guides/github-autodeploys
4. Docker Inc. *Dockerfile reference.* Disponível em:
   https://docs.docker.com/reference/dockerfile/
