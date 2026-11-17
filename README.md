# Ligações no WhatsApp (Android)

Projeto de aplicativo **Android nativo (Java, sem bibliotecas externas)**. Ele:

- solicita permissão para ler o histórico de ligações do próprio aparelho;
- exibe até 1.000 ligações recentes com nome (quando armazenado no histórico), número, data completa e horário;
- informa explicitamente **quem ligou** (você ou outra pessoa), **se você atendeu, perdeu ou recusou a ligação**, e a duração em chamadas concluídas;
- em ligações feitas por você, a duração aparece, mas não afirmamos com certeza que a outra pessoa atendeu (o histórico não confirma isso de maneira padronizada);
- permite buscar e atualizar a lista;
- oferece um botão **WhatsApp** ao lado de cada ligação, sem salvar o contato;
- permite digitar ou corrigir o telefone manualmente (pressione e segure o botão);
- mostra aviso se WhatsApp e WhatsApp Business não estiverem instalados;
- mantém o histórico local: não usa internet, servidor ou analytics.

## Informações de cada ligação

- **Recebidas atendidas:** "Ligaram para você • Atendida".
- **Recebidas perdidas:** "Ligaram para você • Não atendida".
- **Recusadas/bloqueadas:** status explícito.
- **Feitas por você:** "Você ligou" e duração, ou "Atendimento não confirmado" quando duração é zero.
- **Data e hora:** `dd/MM/aaaa às HH:mm`.

O histórico exato disponível é definido pelo celular e suas permissões; algumas entradas não trazem nome ou duração.

## Limitação importante

O WhatsApp não disponibiliza um método público e confiável para um aplicativo comum consultar previamente se **outro número está cadastrado no WhatsApp**. Por isso, o app encaminha o telefone ao WhatsApp. Se o destinatário não tem conta, **o próprio WhatsApp** pode mostrar o aviso correspondente. O aplicativo **não afirma** ter verificado o cadastro do destinatário. A mensagem própria "WhatsApp não instalado" só aparece se não há WhatsApp/WhatsApp Business no **seu celular**.

## Atenção às permissões do Android

`READ_CALL_LOG` é permissão sensível e restrita. É necessário autorizar a leitura do histórico na tela do aparelho. Em algumas versões/dispositivos Android, especialmente com instalação fora da loja, a permissão pode estar bloqueada mesmo após a instalação. Nesse caso, não é possível contornar o bloqueio no código. O Android/Google Play também restringe fortemente a distribuição de apps que acessam histórico de chamadas, e a publicação na loja pode exigir ser o discador padrão ou atender a critérios específicos.

## Como gerar o APK para instalar

1. Instale o **Android Studio** em um computador Windows, Linux ou macOS.
2. Abra a pasta `LigacoesWhatsApp` com **Open**.
3. Nas versões de Android Studio em que o projeto precisar de wrapper Gradle, use a opção de configurar/gerar o wrapper do Gradle 8.9. O arquivo `gradle/wrapper/gradle-wrapper.properties` já aponta para a versão; o JAR do wrapper não está incluído neste ZIP.
4. Aguarde o sincronismo do Gradle e do Android SDK 35 (é necessário acesso à internet no computador).
5. Use **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
6. O arquivo ficará em `app/build/outputs/apk/debug/app-debug.apk`.
7. Transfira esse APK para o celular e instale apenas se confiar no projeto. Autorize a permissão de histórico de ligações quando for solicitada.

**Não há APK pré-compilado neste ZIP:** este ambiente não dispõe do Android SDK e do Gradle necessários para compilá-lo. Portanto, o arquivo ZIP não pode ser instalado diretamente no celular.

## Detalhes técnicos

- Android mínimo: 8.0 (API 26)
- `compileSdk`: 35; `targetSdk`: 34 (build pessoal fora da Play Store)
- Java 17, Android Gradle Plugin 8.7.3, Gradle 8.9
- `READ_CALL_LOG` solicitado somente após toque em Atualizar.
- Para números locais brasileiros, adiciona `55` ao telefone quando já há DDD. Para telefones sem DDD, pede confirmação.
- O link usa `whatsapp://send?phone=...`, com fallback `https://wa.me/...`, associado ao WhatsApp instalado.

Este projeto não faz ligações, não lê conversas, não acessa contatos separadamente e não envia histórico para terceiros.

## Alternativa para gerar APK **sem computador** (GitHub Actions)

O arquivo `.github/workflows/gerar-apk.yml` foi incluído para compilar o APK no serviço GitHub Actions (exige conta gratuita no GitHub e internet). O projeto ainda não foi compilado nem testado por este serviço.

1. No navegador do celular, entre no [GitHub](https://github.com) e crie um repositório privado, vazio, chamado por exemplo `LigacoesWhatsApp`.
2. Descompacte este ZIP. Envie **os arquivos e as pastas de dentro de `LigacoesWhatsApp`**, preservando a estrutura e a pasta oculta `.github/workflows/`. O GitHub pode exigir o navegador em modo para computador para enviar vários arquivos; o `.github` deve constar na raiz do repositório.
3. Vá na aba **Actions**, abra **Gerar APK Android** e clique **Run workflow**. O fluxo também deve iniciar automaticamente ao enviar os arquivos à branch `main`.
4. Após a execução ser concluída com sucesso, abra o resultado e baixe o artefato **LigacoesWhatsApp-APK**. O download vem como ZIP contendo `app-debug.apk`.
5. Extraia `app-debug.apk` e instale-o no Android; o sistema poderá pedir autorização para instalar aplicativos dessa origem.

Se aparecer erro de compilação, o registro do GitHub Actions mostrará o motivo. O APK de *debug* é assinado automaticamente com chave de desenvolvimento. **A permissão READ_CALL_LOG é sensível e pode estar restrita no aparelho**, mesmo com o APK instalado. Não é garantido que o histórico seja acessível em todos os Android.
