# WunderGroundGod
Access live logs of devises using the Wunder Ground API and sends the values to an influx db

# Description

Java programm which reads wheater data from the wunderGround API. It sends the values to an influx db (the influx API v2 is used, which is compatible with Influxdb2 and Influxdb 1.x).

## Docker Image

You can find the Docker image on [Docker Hub](https://hub.docker.com/repository/docker/pendl2/wunder-ground-god)

If you want to run a container, you can use the command `docker run pendl2/wunder-ground-god`

There are different tags available for common processor architectures:
* latest
* the project-version itself

## Configuration

Two Environment Variables are mandatory. These are:
* DEVICE.ID
* API.KEY

Docker Environment Variable | Default | Description
------------ | ------------- | -------------
LOG.LEVEL | INFO | Log Level, can be switched to debug, for detailed information
POLL.TIME.MS | 10000 | Poll Time in ms
DEVICE.ID |  | Device from which the data should be fetched
API.KEY |  | the API key for WunderGround
UNIT | m |  e = english units, m = metric units, h = hybrid units
INFLUXDB.HOST | localhost | The host where the influx service is running
INFLUXDB.PORT | 8086 | The port where the service is running
INFLUXDB.BUCKET | wheater  | The bucket
INFLUXDB.ORG | pendulum | The organisation which is sent to the InfluxDB
INFLUXDB.USER.TOKEN | | If this environment is set, the token will be added to the request, if not, no authentication is used

Example:
setting the host 0.0.0.0 via environment: `sudo docker run -e DEVICE.ID=bla -e API.KEY=blub pendl2/wunder-ground-god`

## Multi-Container Configuration

I created a multi-container configuration, which contains:
* InfluxDb
* Grafana
* Wunder Ground God (this project)

You can find the details in the file: [Multi Container Configuration](https://github.com/lukeSky3434/wunder-ground-god/compose.yaml)
