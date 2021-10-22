# playframework_websocket_redis
PlayFrameworkでRedis PubSubを利用したWebSocketサーバを実装

## 起動時の注意

- ローカルでRedis Serverを立ち上げておく
- 他マシンからアクセスするなら `public/javascripts/chat.js:26` のipアドレスを自マシンのipに書き換えること

## 実行手順

1. sbt runする
2. ブラウザから `http://<自マシンのip>:9000/` にアクセスする

## APIでのチャット送信

`POST: /chat/:roomId/:message` で指定のチャットルームへメッセージを送信できる

e.g.) チャットルームAに「テスト」と送信する場合
  
`POST: http://localhost:9000/chat/A/テスト`

