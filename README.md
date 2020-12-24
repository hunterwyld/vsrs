## VSRS

VSRS(Very Simple Rtmp Server) is a rtmp server written in java.

### Start server
```
mvn clean package
java -jar rtmpserver.jar
```

### Publish/Play stream
```
ffmpeg -re -i video.flv -f flv -c copy "rtmp://127.0.0.1/live/stream"
ffplay "rtmp://127.0.0.1/live/stream"
```
or use publisher like OBS, player like VLC

### Publish/Play text messages
```
java -jar rtmpclient.jar -publish text.json "rtmp://127.0.0.1/live/text"
java -jar rtmpclient.jar -play "rtmp://127.0.0.1/live/text"
```