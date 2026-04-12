# YoutubeMusic Helper para Alexa 🎵

Uma aplicação super leve e privada que transforma seu computador de casa no cérebro de uma rádio inteligente para a sua Alexa. 
Diga para a sua Alexa tocar qualquer música do YouTube e ela continuará tocando outras músicas relacionadas de forma infinita, simulando uma rádio real!

## 🌟 Novidades da Versão Atual
- **Rádio Infinita (Autoplay):** A skill varre os Mixes do YouTube (via `yt-dlp`) e enfileira a próxima música silenciosamente antes da música atual acabar.
- **Ecossistema Completo de Mídia:** Agora suporta parar, pausar, voltar depois de uma pausa, e pular para as próximas/músicas anteriores.
- **Modernizado:** Atualizado para **Java 21** e **Spring Boot 3**. Sem necessidade de configurar banco de dados!

---

## 🏗️ Como Funciona? 

O fluxo é muito simples:
1. Você dá um comando de voz para a Alexa (Ex: _"Alexa, toque Linkin Park"_).
2. A Nuvem da Alexa envia essa mensagem via internet para o **seu computador** com a ajuda do **Ngrok**.
3. O Java no seu computador executa a ferramenta **`yt-dlp`**, que encontra a música no YouTube Music (com a melhor qualidade de áudio).
4. O link do áudio é enviado devolta para a sua caixinha Echo, que passa a tocar a música.
5. Quando a música estiver acabando, o ciclo se repete automaticamente, trazendo a próxima música relacionada.

Você pode decidir entre usar o **Modo 1 PC** (deixar o programa rodando no computador em que você trabalha) ou no **Modo 2 PCs** (compilar e colocar pra rodar em um servidor ou notebook velho em casa).

---

## ⚙️ Pré-requisitos (No computador que ficará rodando)

1. **Java 21 (JDK)** instalado.
2. **`yt-dlp`**: Esta é a ferramenta que extrai as músicas do YouTube.
   - Baixe o executável `yt-dlp` (para Windows `.exe`, ou binário para Linux/Mac) no [GitHub Oficial](https://github.com/yt-dlp/yt-dlp/releases).
   - É **obrigatório** adicionar a pasta onde o `yt-dlp` está salvo nas variáveis de ambiente `PATH` do sistema para que o Java o encontre.
3. **Ngrok**: O túnel que vai abrir sua porta local para a internet.
   - Baixe no [Ngrok](https://ngrok.com/), instale e adicione o seu Token (`ngrok config add-authtoken SEU_TOKEN`).

---

## 🚀 Ligando o Servidor

Se tiver o Git e o Maven configurados, simplesmente abra o projeto e rode via terminal:
```bash
mvn spring-boot:run
```

Ou então, compile o pacote (`mvn clean package`) e rode o compilado:
```bash
java -jar target/alexamusic-0.0.1-SNAPSHOT.jar
```

Com o script rodando na porta `8080`, abra outro terminal e execute o túnel:
```bash
ngrok http 8080
```
O Ngrok fornecerá um link verde (Ex: `https://alguma-coisa.ngrok-free.app`). O endereço oficial que conectará a Alexa à sua máquina será este link com **`/alexa`** no final! \
👉 Exemplo final: `https://alguma-coisa.ngrok-free.app/alexa`

---

## 🎙️ Configurando na Alexa (Developer Console)

Fique tranquilo, criar sua Skill pessoal gratuita e vitalícia não fere direitos do YouTube, pois **não iremos publicá-la**.

1. Acesse: [Alexa Developer Console](https://developer.amazon.com/alexa/console/ask).
2. Vá em **Create Skill** > Escolha um Nome (Ex: Music Helper) > **Custom** > **Provision your own**.
3. Na barra lateral (Interaction Model -> Skill Invocation Name), coloque o que deseja falar para abrir o app. Ex: `youtube music`.
4. Em **Interfaces** na barra lateral esquerda, ative o switch do **Audio Player** para autorizarmos músicas em stream.

### 📝 Criando os Comandos de Voz (Intents)
Em `Interaction Model > Intents`, você precisa adicionar algumas intenções. Em **Sample Utterances**, registre frases de ensino.
1. Crie uma ação chamada **`PlayIntent`** e dentro adicione o Slot **`SongName`** (tipo `AMAZON.SearchQuery`). Frases de exemplo:
   - _"tocar {SongName}"_
   - _"quero ouvir {SongName}"_
2. Em Intents, clique no botão para adicionar os Intents built-in da Amazon:
   - Adicione `AMAZON.PauseIntent`
   - Adicione `AMAZON.ResumeIntent`
   - Adicione `AMAZON.NextIntent` (Para "Próxima")
   - Adicione `AMAZON.PreviousIntent` (Para "Voltar")
3. Clique em **Save Model** e em **Build Model** (Botão grande em cima).

### 🔌 Encerrando o Vínculo (Endpoint)
Vá ao painel da esquerda até a aba inferior de **Endpoint**.
Selecione **HTTPS**.
Para a região Padrão (Default Region), cole a URL completa do seu tunel ngrok, ex:
`https://alguma-coisa.ngrok-free.app/alexa`
No campo certificado de lista suspensa logo abaixo, escolha "My development endpoint is a sub-domain of a domain that has a wildcard certificate...". Salve.

## 🎧 Divirta-se!

Pronto! Diga na sala de casa: `Alexa, peça para o youtube music tocar Bohemian Rhapsody`
A magia começa, o computador aciona o youtube de forma fantasma e manda música na melhor taxa de bits pra sua casa junto com os comandos de rádio.
