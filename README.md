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

You can find the details in the file: [Multi Container Configuration](compose.yaml)


The [compose.yaml](compose.yaml) includes the service for the influxdb and grafana. As well the wunder-ground-god service will be started, which collects the data.

### Configuration

Environment Variable | Default | Description
------------ | ------------- | -------------
INFLUX_DB_HOME | /home/pi/influxdb/ | Variable which points influxdb home directory
GRAFANA_HOME | /home/pi/grafana/ | Variable which points to the grafana home directory

Take care, that you don't forget to fill in the API.KEY and DEVICE.ID in the compose file!

### Precondition

There should be three directories available in the HOME directory of influx:
* *data*: the docker container will put the data of influx in this directory
* *config*: config file can be added here (e.g.: influxdb.conf)
* *backup*: backup directory

In the HOME directory of grafana there should be one directory:
* *data*: the docker container will put the data of grafana in this directory

## Docker-Compose

Compose is a tool for defining and running multi-container Docker applications, which uses a YAML configuration file.

To install the compose tool on your maschine follow the [instructions](https://docs.docker.com/compose/install/).

For the start the containers use the `docker-compose up` command, add the `-d` option to run the containers in the background.

To stop and remove everything, the `docker-compose down` command should be used.
