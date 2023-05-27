# FileFerry

ダウンロードフォルダを監視し、ダウンロードされたファイルを半自動で任意のフォルダに振り分けるツール。  
A tool that monitors download folders and semi-automatically distributes downloaded files to an arbitrary folder

- Monitored directory(Mac OS)：/Users/"users.home"/Downloads

## Usage

```
$ gradle build
$ java -jar app/build/libs/app.jar
```

デフォルトでディレクトリを設定します。`FileFerry/app/src/main/resources/paths.txt`を書き換えてください。  
複数のディレクトリを設定する場合は、ディレクトリごとに改行してください。

### "paths.txt" example

```
/path/to/default/directory/one
/path/to/default/directory/two
```
