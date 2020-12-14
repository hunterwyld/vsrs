## VSRS

VSRS(Very Simple Rtmp Server) is a rtmp server written in java.

### Start server
```
mvn clean package
java -jar target/vsrs.jar
```

### Publish/Play stream
```
ffmpeg -re -i video.flv -f flv -c copy "rtmp://127.0.0.1/live/stream"
ffplay "rtmp://127.0.0.1/live/stream"
```
