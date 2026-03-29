# Alexa Music Helper (Spring Boot + yt-dlp)

Esta aplicação atua como um Webhook local via ngrok para uma **Alexa Custom Skill**. Quando você diz para a Alexa "toque [Nome da Música]", a Skill consulta essa aplicação Spring Boot, que invoca o `yt-dlp` localmente para pesquisar a melhor qualidade de áudio no YouTube Music e retorna essa URL diretamente para o alto-falante Echo.

## Pré-requisitos (Para Windows)

Como o aplicativo utiliza o terminal em background para rodar o `yt-dlp`, precisamos garantir que o mesmo está instalado e no seu PATH do Windows. Além disso, precisamos do ngrok para a Alexa enxergar o seu computador de fora.

### 1. Instalando o `yt-dlp` e o FFmpeg
O `yt-dlp` exige acesso a decodificadores para obter áudio cru das plataformas. O mais famoso é o FFmpeg.
1. Baixe o **yt-dlp.exe** diretamente do [GitHub Oficial](https://github.com/yt-dlp/yt-dlp/releases). Salve-o numa pasta (ex: `C:\yt-dlp`).
2. Adicione a pasta ao PATH do Windows (Pesquise no menu iniciar: "Editar variáveis de ambiente").
3. Instale o **FFmpeg**. A forma mais fácil é via [gyan.dev](https://www.gyan.dev/ffmpeg/builds/) (baixar o zip release-essentials, extrair, e adicionar a pasta `bin` ao PATH).
4. Abra o CMD (Prompt de Comando) e digite `yt-dlp --version`. Ele deve retornar um número da versão sem erros.

### 2. Instalando o Ngrok
1. Crie uma conta no [Ngrok](https://ngrok.com/).
2. Baixe o executável para Windows.
3. Obtenha seu Token authtoken na dashboard e rode: `ngrok config add-authtoken SEU_TOKEN`.

---

## Rodando o Projeto Localmente

No terminal (CMD, PowerShell ou no próprio Eclipse/IntelliJ), dentro desta pasta (`alexa-music-helper`), digite o comando do Maven. (A porta padrão subirá em `8080`).

```bash
mvn spring-boot:run
```

Com a aplicação de pé, abra um segundo terminal e execute o ngrok abrindo a internet para a porta 8080 local:

```bash
ngrok http 8080
```

Ele gerará um painel. **Copie o endereço Forwarding**, que será algo como: `https://abcd-123.ngrok-free.app`.
O seu Webhook endpoint final será `https://abcd-123.ngrok-free.app/alexa`. 

---

## Criando a Sua Skill de Graça (Alexa Developer Console)

Como essa Skill não vai ser publicada na loja oficial, ela é **gratuita** de se criar.

1. Acesse: [Alexa Developer Console](https://developer.amazon.com/alexa/console/ask).
2. Clique em **Create Skill**.
3. Escolha **Other** > **Custom**. E em "Choose a method to host your skill" escolha **Provision your own**.
4. Defina um nome, ex: "Music Helper".
5. Na lateral esquerda, vá em **Interaction Model > Invocation**. Preencha **Skill Invocation Name** como `music helper`.

### Configurando o Modo Audioplayer
1. Você precisa avisar a Alexa que sua Skill emite som contínuo. No menu esquerdo, vá no final em **Interfaces**.
2. Ligue a chave de **Audio Player** e clique em **Save Interfaces**.

### Criando a PlayIntent e seus Enunciados
1. Vá em **Interaction Model > Intents**.
2. Clique em **Add Intent**. Crie uma Custom Intent chamada **PlayIntent**.
3. Role até **Intent Slots** e crie um novo Slot chamado `SongName`. Escolha o Slot Type como `AMAZON.SearchQuery` ou `AMAZON.MusicRecording`.
4. Volte nas **Sample Utterances** (Frases de Exemplo) para o `PlayIntent` no topo, e escreva algumas amostras para treinar a IA, referenciando a variável com chaves `{}`, como:
   - `tocar {SongName}`
   - `toque {SongName}`
   - `quero ouvir {SongName}`
   - `põe a música {SongName}`
5. Clique em **Save Model**, e depois no botão grande superior **Build Model** (Isso leva uns bons 30-60 segundos).

### Conectando a Alexa ao seu PC (Webhook via ngrok)
1. No menu lateral da página, acesse **Endpoint**.
2. Selecione a opção da bolinha chamada **HTTPS**.
3. Cole no *Default Region*: `https://seu-endereco-ngrok.ngrok-free.app/alexa` (Não esqueça o /alexa no final).
4. **IMPORTANTE:** No dropdown chamado *Select SSL certificate type*, selecione a segunda opção: **"My development endpoint is a sub-domain of a domain that has a wildcard certificate from a certificate authority"**. (Essa chave diz que já confiamos no certificado base da provedora do Ngrok).
5. Salve o Endpoint.

## 🎵 Tá pronto! Testando!
Vá para a aba **Test** na Developer Console e mude o dropdown esquerdo de "Off" para *Development*.
Você pode testar ali pelo microfone virtual, ou como a conta de desenvolvedor da Alexa já é a sua mesma conta da Amazon vinculada no seu dispositivo físico, você pode simplesmente usar o seu Echo bolinha em cima da mesa!

Diga: **"Alexa, fale com Music Helper para tocar The Strokes."**

A tela do console ou a caixa de som indicará sucesso tocando em segundos! Você só precisará manter os terminais com `Spring` e `ngrok` ligados no PC para ouvir!
