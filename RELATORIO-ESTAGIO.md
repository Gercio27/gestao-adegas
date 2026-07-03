# Relatório de Estágio Curricular
## Desenvolvimento de um sistema de gestão de pequenas adegas

> Documento de apoio ao relatório de estágio. Descreve o projeto desenvolvido,
> as tecnologias utilizadas, a arquitetura, as funcionalidades, as competências
> adquiridas e as dificuldades ultrapassadas.

---

## 1. Identificação do projeto

| | |
|---|---|
| **Título** | Software de Gestão de Pequenas Adegas — "Da vinha ao produto acabado" |
| **Entidade de acolhimento** | ACV – Produção e Comércio de Vinhos de Talha, Lda (Beja / Vila de Frades) |
| **Tipo** | Estágio curricular |
| **Área** | Engenharia Informática / Desenvolvimento de Software |
| **Tecnologia principal** | Aplicação Web em Java (Spring Boot) com base de dados MySQL |

---

## 2. Enquadramento e motivação

A ACV é uma empresa produtora de vinhos de talha, um método tradicional do
Alentejo. Todo o processo produtivo — desde a análise da uva na vinha até à
entrega do vinho engarrafado ao cliente — era registado de forma manual e
dispersa, o que dificultava a rastreabilidade, o controlo de existências e o
cumprimento das exigências dos organismos oficiais (IVV/CVR).

O objetivo do estágio foi **conceber e desenvolver uma aplicação informática**
que centralizasse toda essa informação, seguindo um princípio orientador
definido pela empresa: *"registar logo quando acontece, uma só vez, toda a
informação"*.

---

## 3. Objetivos

- Registar todas as **variáveis** (fichas) que entram no processo produtivo, cada
  uma com um código único gerado automaticamente pelo sistema.
- Modelar os **processos por fases** (da maturação da uva ao setor comercial),
  com abertura e fecho de cada operação.
- Garantir a **rastreabilidade** completa: da uva ao mosto, ao vinho e à garrafa.
- Implementar **regras de negócio** críticas: controlo de capacidade dos
  recipientes, controlo de stock, e distinção entre bens próprios e de terceiros.
- Produzir **documentos** (nota de entrega, documento de acompanhamento e
  etiquetas) e **listagens/mapas** de apoio à gestão.
- Assegurar o **controlo de acessos** com perfis de utilizador (operador e
  administrador).

---

## 4. Tecnologias e ferramentas utilizadas

| Tecnologia | Função no projeto |
|---|---|
| **Java 17** | Linguagem de programação (backend). |
| **Spring Boot 3.3** | Framework principal; arranque, injeção de dependências, servidor web embutido (Tomcat). |
| **Spring MVC** | Camada web (controladores, rotas HTTP). |
| **Spring Data JPA / Hibernate** | Mapeamento objeto-relacional (ORM) e acesso à base de dados. |
| **Spring Security** | Autenticação, perfis de acesso e proteção das páginas. |
| **Thymeleaf** | Motor de templates para gerar as páginas HTML no servidor. |
| **Bootstrap 5** | Estilo e responsividade da interface (servido localmente, sem internet). |
| **MySQL / MariaDB** | Base de dados relacional (produção). |
| **H2 Database** | Base de dados em memória, usada em modo de demonstração/testes. |
| **Maven** | Gestão de dependências e construção do projeto (empacotamento em `.jar`). |
| **Git** | (Recomendado) Controlo de versões. |

**Justificação das escolhas:** o Spring Boot foi escolhido por ser o padrão da
indústria para aplicações Java empresariais, permitindo desenvolver rapidamente
uma aplicação robusta com segurança, acesso a dados e interface web num único
projeto. O Thymeleaf (páginas geradas no servidor) simplificou o
desenvolvimento face a uma solução com frontend separado, adequando-se a uma
equipa pequena.

---

## 5. Arquitetura da solução

A aplicação segue o padrão **MVC (Model–View–Controller)** em camadas:

```
Navegador (utilizador)
        │  HTTP
        ▼
Controladores (Spring MVC)  ──►  Serviços (regras de negócio, @Transactional)
        │                               │
        ▼                               ▼
Templates Thymeleaf (Vistas)      Repositórios (Spring Data JPA)
                                        │
                                        ▼
                                  Base de dados MySQL
```

- **Entidades (Model):** representam as fichas, os processos e os produtos
  (ex.: `Talha`, `ProcessoVindima`, `Mosto`). Todas herdam de uma classe base
  que fornece o **código automático** e as datas de criação/atualização.
- **Repositórios:** interfaces do Spring Data JPA que geram automaticamente as
  consultas à base de dados.
- **Serviços:** concentram a lógica de negócio mais sensível (ex.: fecho de uma
  moagem), com transações **tudo-ou-nada** (`@Transactional`).
- **Controladores:** recebem os pedidos, invocam os serviços/repositórios e
  escolhem a página a apresentar.
- **Vistas (Thymeleaf):** as páginas HTML, reutilizando fragmentos comuns
  (cabeçalho, menu, rodapé).

A configuração é feita por **perfis** (`dev` para H2, `mysql` para produção) e
por **variáveis de ambiente**, o que permite usar a mesma aplicação em ambientes
diferentes sem alterar o código.

---

## 6. Funcionalidades implementadas

### 6.1 Fichas (as "variáveis" do sistema)
Castas (com lista nacional pré-carregada), Vinhas (com parcelas, casta e área),
Trabalhadores, Fornecedores, Adegas, Talhas, Depósitos/Cubas e Consumíveis
(garrafas, rolhas, rótulos, cápsulas, caixas, etiquetas) — todos com **código
automático** e histórico.

### 6.2 Processos por fases (1 a 8)
| Fase | Processo | Efeito ao fechar |
|---|---|---|
| 1 | Análise à maturação + Planeamento dos vinhos | Boletim de análise; mapa de planeamento que absorve as análises |
| 2 | Vindima | Registo da colheita e origem da uva |
| 3 | Moagem e enchimento de talhas | **Gera automaticamente as fichas de mosto** e soma os litros |
| 4 | Fermentação (remontagens, atestos, entradas/saídas, passagem a vinho) | Controlo de capacidade; passagem a vinho a granel |
| 5 | Certificação (a granel e engarrafado) | Marca o produto como certificado |
| 6 | Engarrafamento | **Baixa** de vinho, garrafas e rolhas; cria o vinho engarrafado |
| 7 | Rotulagem/embalamento | Baixa de rótulos/cápsulas/caixas |
| 8 | Passagem ao comercial | Baixa de stock e emissão da **Nota de Entrega** |

### 6.3 Produtos gerados automaticamente
Mostos, Vinhos a granel, Vinhos engarrafados e Lotes.

### 6.4 Documentos e mapas
- **Documentos imprimíveis (PDF via navegador):** Nota de Entrega, Documento de
  Acompanhamento (DA) e Etiquetas para talhas/depósitos/contentores.
- **Painel de gestão:** existências, ocupação de talhas/depósitos, alertas de
  stock baixo e processos em curso.
- **Listagens por datas** e **mapa de planeamento**.

---

## 7. Regras de negócio implementadas

- **Código automático:** cada ficha/processo recebe um identificador único
  (ex.: `VDM-000001`), gerado com bloqueio para evitar duplicados.
- **Controlo de capacidade:** o sistema impede encher uma talha/depósito acima
  da sua capacidade; nos atestos, valida os **dois** lados (o que recebe e o que
  cede).
- **Controlo de stock:** o engarrafamento e a rotulagem não permitem usar mais
  garrafas/rolhas/rótulos do que os existentes.
- **Rastreabilidade:** a origem da uva acompanha o produto até à garrafa e ao
  cliente.
- **Próprio vs. terceiros:** distinção em vinhas, recipientes e produtos.
- **Reversibilidade:** o administrador pode reabrir um processo fechado, sendo os
  efeitos (mostos, volumes, stocks) **revertidos** de forma transacional.
- **Controlo de acessos:** o operador vê apenas os processos que abriu; o
  administrador vê tudo e gere os utilizadores.

---

## 8. Metodologia de desenvolvimento

O desenvolvimento foi **incremental**, entregando primeiro um MVP (fichas +
autenticação) e acrescentando depois cada fase produtiva, sempre com um ciclo
de **desenvolver → compilar → testar → validar** antes de avançar. Cada
funcionalidade foi testada através de pedidos HTTP simulando a utilização real
(login, criação de registos, fecho de processos, verificação dos efeitos).

---

## 9. Testes e validação

Cada módulo foi validado funcionalmente. Exemplos:
- Confirmação de que o fecho de uma moagem gera os mostos e soma os volumes;
- Rejeição correta de um atesto quando a origem não tem litros suficientes ou o
  destino excede a capacidade;
- Baixa correta de stock no engarrafamento e rejeição por stock insuficiente;
- Persistência dos dados em MySQL (confirmada reiniciando a aplicação);
- Isolamento de acessos entre operador e administrador.

---

## 10. Dificuldades encontradas e soluções

- **Carregamento tardio de coleções (LazyInitializationException):** ao mostrar
  listas ligadas (ex.: enchimentos, parcelas) as coleções não carregavam durante
  a construção da página. Resolvido ativando o carregamento na camada de vista
  (`open-in-view`), adequado a uma aplicação com páginas geradas no servidor.
- **Geração de código duplicado:** um formulário enviava o campo de código como
  texto vazio (`""`) em vez de nulo, fazendo o sistema tentar inserir códigos
  repetidos. Resolvido passando a decidir a geração do código pela ausência de
  identificador (`id`), tal como nos restantes módulos.
- **Configuração para vários ambientes:** para funcionar tanto localmente
  (XAMPP) como num alojamento, a configuração da base de dados e das
  palavras-passe foi externalizada em **variáveis de ambiente** com valores por
  omissão.
- **Distribuição sem instalação:** para facilitar a utilização pela empresa, a
  aplicação foi empacotada num executável `.jar` com um Java portátil embutido e
  um ficheiro de arranque (`INICIAR.bat`), evitando instalações complexas.

---

## 11. Competências adquiridas (o que aprendi)

Durante o estágio consolidei e adquiri as seguintes competências:

**Técnicas**
- Desenvolvimento de aplicações web com **Java e Spring Boot** (Spring MVC,
  injeção de dependências, perfis de configuração).
- **Mapeamento objeto-relacional** com JPA/Hibernate: entidades, relações
  (um-para-muitos, muitos-para-um), herança e consultas derivadas.
- **Modelação de base de dados** relacional e compreensão da relação entre o
  modelo de objetos e as tabelas.
- **Transações** e integridade dos dados (`@Transactional`), garantindo
  operações tudo-ou-nada em regras de negócio críticas.
- **Segurança** de aplicações: autenticação, perfis/autorização e proteção
  contra CSRF com Spring Security.
- Desenvolvimento de **interface web** com Thymeleaf e Bootstrap, incluindo
  formulários dinâmicos e documentos imprimíveis.
- **Empacotamento e distribuição** de uma aplicação Java (Maven, `.jar`,
  Docker) e noções de **alojamento** (variáveis de ambiente, base de dados
  gerida, HTTPS).
- **Depuração** de problemas reais e leitura de mensagens de erro/registos.

**Transversais**
- Tradução de **requisitos de negócio** (o caderno de encargos da empresa) em
  funcionalidades concretas.
- Trabalho **incremental e organizado**, com validação contínua.
- Documentação técnica e comunicação com a entidade de acolhimento.
- Preocupação com a **experiência do utilizador** e com a **segurança**.

---

## 12. Instalação e execução

**Local (na adega):**
1. Base de dados MySQL (ex.: através do XAMPP), iniciada.
2. Executar a aplicação (`INICIAR.bat`) — a base de dados e as tabelas são
   criadas automaticamente no primeiro arranque.
3. Aceder em `http://localhost:8080` (utilizador inicial `admin`).

**Perfis:** `mysql` (produção, dados persistentes) e `dev` (H2 em memória, para
demonstração).

---

## 13. Trabalho futuro

- Alojamento na Internet (plataforma que suporte Java/Docker, com base de dados
  gerida e HTTPS).
- Testes automatizados (unitários e de integração).
- Ligação eletrónica aos sistemas do IVV/CVR (atualmente o registo é interno).
- Relatórios/estatísticas adicionais e exportação de dados.

---

## 14. Conclusão

O estágio permitiu desenvolver, do início ao fim, uma aplicação real e completa
para um problema concreto de uma empresa. O sistema cobre todo o ciclo produtivo
da adega, das análises à maturação até à entrega ao cliente, implementando
regras de negócio exigentes (capacidade, stock, rastreabilidade) e boas práticas
de desenvolvimento (arquitetura em camadas, transações, segurança).

Mais do que a componente técnica, este projeto proporcionou a experiência de
**transformar as necessidades de um cliente numa solução funcional**, validada e
pronta a ser utilizada — uma aproximação realista ao trabalho de um engenheiro
de software.

---

*Documento elaborado no âmbito do estágio curricular — projeto de gestão da ACV –
Produção e Comércio de Vinhos de Talha, Lda.*
