# YoutubeMusic--for-Alexa (Spring Boot + yt-dlp)

Esta aplicação atua como um "cérebro" privado (Webhook) via ngrok para uma **Alexa Custom Skill**. 
Quando você dá um comando para a sua caixa de som Alexa, a Skill consulta essa aplicação Spring Boot rodando no seu computador. O Java então invoca a ferramenta `yt-dlp` silenciosamente para pesquisar a melhor qualidade de áudio da música solicitada no YouTube Music, e retorna a URL desse áudio em tempo real diretamente para o seu dispositivo Echo tocar.

---

## 🏗️ Arquitetura do Projeto (1 PC vs 2 PCs)

O projeto é incrivelmente leve e flexível. Você pode optar por duas formas diferentes de uso baseadas na sua disponibilidade de hardware:

### 🖥️ Modo 2 Computadores (Recomendado para quem tem um PC sobrando)
Foi assim que essa Skill nasceu! 
* **Máquina 1 (Linux/Dev):** Usada exclusivamente para escrever o código e compilar o projeto (rodar o comando Maven `mvn clean package`). Após gerar o arquivo final `.jar`, você o transfere (via pendrive, nuvem, etc) para a segunda máquina.
* **Máquina 2 (Windows Servidor):** Um notebook ou PC que possa ficar mais tempo ligado. Ele apenas recebe o `.jar` pronto, o `yt-dlp` e roda o `.bat` para iniciar o Java e o Ngrok simultaneamente. A Alexa se comunica apenas com esta máquina cruzando a ponte do Ngrok.

### 💻 Modo 1 Computador (Tudo em um só lugar)
* **Máquina Única (Windows, Mac ou Linux):** Você faz tudo na mesma máquina. Programa no VSCode, compila usando o Maven, e deixa o Ngrok rodando no seu próprio terminal em background enquanto trabalha ou estuda. Simples e direto.

---

## ⚙️ Pré-requisitos (Para a Máquina Servidor)

A máquina que vai ficar **rodando** o projeto e aguardando chamadas da Alexa precisa ter:

1. **Java 17 ou superior** instalado (JRE ou JDK).
2. **`yt-dlp`**: Esta é a ferramenta que extrai as músicas do YouTube.
   - Baixe o executável `yt-dlp` (para Windows `.exe`, ou binário para Linux) no [GitHub Oficial](https://github.com/yt-dlp/yt-dlp).
   - É **obrigatório** adicionar a pasta onde o `yt-dlp` está salvo nas variáveis de ambiente `PATH` do seu sistema operacional para que o Java consiga achá-lo de qualquer lugar.
3. **Ngrok**: O túnel que abre sua porta local para a internet.
   - Crie uma conta no [Ngrok](https://ngrok.com/), instale o programa e autentique usando seu Token (`ngrok config add-authtoken SEU_TOKEN`).

---

## 🚀 Ligando o Servidor localmente

Se você estiver em um terminal rodando direto o código fonte:
```bash
mvn spring-boot:run
```

Se você estiver usando a versão compilada (o arquivo gerado na pasta `/target`):
```bash
java -jar alexamusic-0.0.1-SNAPSHOT.jar
```

Com a aplicação Java de pé (porta padrão 8080), abra um **segundo terminal** e crie o túnel da internet para a porta 8080:
```bash
ngrok http 8080
```

O Ngrok vai te dar um URL verde (Exemplo: `https://abcd-123.ngrok-free.app`). 
Copie este link. O endereço oficial da sua ponte com a Alexa será este link **acrescido de `/alexa`** no final!

*(**Dica Windows:** Crie um arquivo `.bat` contendo os dois comandos juntos para subir o sistema com 1 clique duplo).*

---

## 🎙️ Criando a Skill "Youtube Music" na Alexa (Developer Console)

Esta skill não viola direitos autorais porque você **não a publicará**. É apenas uma skill de desenvolvedor privada amarrada na sua conta, portanto é **vitalícia e gratuita**.

1. Acesse: [Alexa Developer Console](https://developer.amazon.com/alexa/console/ask).
2. Clique em **Create Skill** > **Start from Scratch** > **Custom** > **Provision your own**.
3. **Invocation Name:** Vá no menu esquerdo `Invocations > Skill Invocation Name`. Digite minúsculo: `youtube music`. (Este é o nome que a Alexa vai escutar para ligar o seu código).
4. **Permitindo Músicas Longas:** No menu esquerdo vá em `Interfaces` e ative a chave **Audio Player**. Salve no topo.

### 📝 Ensinando os Comandos Verbais (Intents)
1. Vá em `Interaction Model > Intents` e clique em **Add Intent**.
2. Crie uma ação chamada **`PlayIntent`** (Respeite as maiúsculas).
3. Dentro desta PlayIntent, desça até **Intent Slots** e crie a variável **`SongName`**. Coloque o tipo dela como `AMAZON.SearchQuery` (Isso permite nomes de bandas exóticos).
4. Suba até **Sample Utterances** (Frases de Exemplo) e insira os treinamentos da IA um por um usando sua variável:
   - `tocar {SongName}`
   - `toque {SongName}`
   - `quero ouvir {SongName}`
5. Clique em **Save Model** e no botão azul gigante **Build Model**. Aguarde finalizar.

### 🔌 Conectando à sua Máquina Servidor
1. No menu escuro da esquerda, clique no final em **Endpoint**.
2. Selecione **HTTPS** (Ignorando AWS Lambda).
3. No campo "Default Region", cole a URL completa do seu Ngrok: `https://abcd-123.ngrok-free.app/alexa`.
4. Importante: No menu de certificado logo abaixo, selecione a 2ª opção: *"My development endpoint is a sub-domain of a domain that has a wildcard certificate..."*. Salve tudo no topo.

---

## 🎧 Pronto! Hora da Magia!

Não precisa publicar. Como você logou nesse portal com sua conta de cliente da Amazon, a sua Alexa física (ou o aplicativo Alexa do celular) já absorveu silenciosamente o seu novo código na mesma aba de conta!

No aplicativo ou falando com sua caixa de som na sala, basta dizer:
> **"Alexa, abra youtube music e toque Linkin Park"**

Em meio segundo seu computador vai capturar o texto, chamar o YouTube invisivelmente, extrair a mídia em alta qualidade e empurrá-la direto para os alto-falantes da sua casa. Bom proveito!
