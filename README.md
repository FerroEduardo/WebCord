<p align="center">
    <h1>WebCord</h1>
    <a href="https://github.com/FerroEduardo/DCC-Discord-Bot/actions/workflows/gradle.yml">
        <img src="https://github.com/FerroEduardo/DCC-Discord-Bot/actions/workflows/gradle.yml/badge.svg?branch=main" alt="Java CI with Gradle">
    </a>
</p>

## Desenvolvimento:
O bot foi desenvolvido em:
- Java 11, utilizando o Gradle para as dependências
- [JDA](https://github.com/DV8FromTheWorld/JDA), API do Discord para Java
- [Hibernate](https://hibernate.org/orm/) para ORM e o driver do JDBC para o Postgres

## Como executar:
Na primeira vez que o código for executado, ele irá gerar um arquivo `dcc-bot.json` na pasta onde o código está sendo executado.
Se estiver executando um `.jar`, o .json irá ser gerado na mesma pasta.
Mas es estiver executando pelo `Gradle`, o arquivo será gerado na pasta `build/classes/java/`.  
Esse arquivo terá as chaves necessárias para acessar o banco de dados Postgres e o token do bot do discord.
O valor padrão para todas as chaves é `null` e o código só funcionará se os valores forem substituídos.
```json
{
  "databaseName" : null,
  "databaseUsername" : null,
  "databasePassword" : null,
  "token" : null,
  "timeoutSeconds" : null,
  "schedulerSeconds" : null,
  "websites": {
    "nome_do_site" : "url_do_site",
    "de_outro_site" : "url_de_outro_site"
  }
}
```

Para executar:
```bash
./gradlew run
```

Para gerar um executável .jar:
```bash
./gradlew jar
```