# DailyClean
![workflow](https://github.com/AxaGuilDEv/dailyclean/actions/workflows/dailyclean-docker-images.yml/badge.svg) [![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=AxaGuilDEv_dailyclean&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=AxaGuilDEv_dailyclean) [![Docker API](https://img.shields.io/docker/pulls/axaguildev/dailyclean-api.svg)](https://hub.docker.com/r/axaguildev/dailyclean-api/builds) [![Docker JOB](https://img.shields.io/docker/pulls/axaguildev/dailyclean-job.svg)](https://hub.docker.com/r/axaguildev/dailyclean-job/builds)

![DailyClean](./dailyclean.gif "DailyClean")

- [About](#about)
- [Getting Started](#getting-started)
- [How Does It Work](#how-does-it-work)
- [Contribute](#contribute)
- [Authors](#authors)

## About

Daily clean is all you to turn on or off automatically or manually all your pods your kubernetes namespace.
Save the planet with DailyClean.
Daily clean only use kubernetes native API.

![DailyClean Automation](./dailyclean-configuration.png "DailyClean Automation")

## Getting Started

To test dailyclean on your local machine by using kubernetes with Docker Desktop, please use these commands:

```
git clone https://github.com/AxaGuilDEv/dailyclean.git
cd dailyclean
git checkout demo/devnation
cd demo
# Create dailyclean serviceaccount
kubectl apply -f dailyclean-serviceaccount.yml
# Link your docker account to this service account in order to pull images
oc secret link dailyclean docker --for=pull
# Patch workspace deployment
kubectl apply -f patch-workspace-deployment.yml
# Install dailyclean for the default service account
kubectl apply -f deployment-dailyclean.yml
# Install three instances of kubernetes-bootcamp
kubectl apply -f deployment-others.yml
```

Now, open your favorite browser and enter the url of dailyclean-api service : http://localhost:30001

Enjoy dailyclean !!!!

## How Does It Work

- Daily clean use native kubernetes API, it works with any kubernetes projects. 
DailyClean is a pod that have to be install in your namespace. 
It create cron job that start or stop your pods. 
- API is in native GraalVM so it is lightweight.
- User interface is in React/Javascript.

## Contribute

- [How to run the solution and to contribute](./CONTRIBUTING.md)
- [Please respect our code of conduct](./CODE_OF_CONDUCT.md)

## Authors

The awesome team :

- __[Thomas Lemarchand](https://github.com/tlemarchand)__ : Conception, Architecture, Dailyclean job developer
- __[Pierre-Henri Gache](https://github.com/phgache)__ : Architecture, CI/CD Pipelines
- __[Guillaume Thomas](https://github.com/guillaume-thomas)__ : API developer
- __[Guillaume Chervet](https://github.com/guillaume-chervet)__: Frontend developer, CI/CD Pipelines
- Charles Herriau: Developer of the statefulset management feature
