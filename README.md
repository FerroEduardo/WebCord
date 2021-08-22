# DCC-Discord-Bot

## Desenvolvimento:
O bot foi desenvolvido em:
- Java 11, utilizando o Gradle para as dependências
- [JDA](https://github.com/DV8FromTheWorld/JDA), API do Discord para Java
- [Hibernate](https://hibernate.org/orm/) para ORM e o driver do JDBC para o Postgres

## Como executar:
Na primeira vez que o código for executado, ele irá gerar um arquivo `dcc-bot.properties` na pasta onde o código está sendo executado.
Se estiver executando um `.jar`, o .properties irá ser gerado na mesma pasta.
Mas es estiver executando pelo `Gradle`, o arquivo será gerado na pasta `build/classes/java/`.  
Esse arquivo terá as chaves necessárias para acessar o banco de dados Postgres e o token do bot do discord.
O valor padrão para todas as chaves é `NULL` e o código só funcionará se os valores forem substituídos.
```properties
database_name=NULL
database_username=NULL
database_password=NULL
token=NULL
```

Para executar:
```bash
./gradlew run
```

Para gerar um executável .jar:
```bash
./gradlew jar
```