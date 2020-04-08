# OpenShift and Jenkins on IBM Cloud

![IBM](../../images/os-logo.jpg?raw=true "IBM")

[Red Hat OpenShift on IBM Cloud](https://cloud.ibm.com/docs/openshift?topic=openshift-why_openshift)is an extension of the IBM Cloud Kubernetes Service, where IBM manages OpenShift Container Platform for you.

With Red Hat OpenShift on IBM Cloud developers have a fast and secure way to containerize and deploy enterprise workloads in Kubernetes clusters. OpenShift clusters build on Kubernetes container orchestration that offers consistency and flexibility for your development lifecycle operations.

This tutorial wants to help you as a developer to become familiar with Continuous Integration / Continuous Delivery pipelines using OpenShift Jenkins Plugin on Red Hat OpenShift 4.3.

## Prerequisites

* Register for an [IBM Cloud account](https://cloud.ibm.com/registration).
* Create an [ OpenShift v4.3 cluster in IBM Cloud](https://cloud.ibm.com/docs/openshift?topic=openshift-openshift_tutorial) 
* Install and configure [IBM Cloud CLI](https://cloud.ibm.com/docs/cli?topic=cloud-cli-getting-started#overview)
* Optional : download [Visual Studio Code IDE](https://code.visualstudio.com) for editing the NodeJs project


## Estimated time 

It should take you approximately 20 minutes to run these tutorials on OpenShift 4.2

## Tutorials

* [OpenShift & S2I (Source to Image) - build and deploy a NodeJs application using s2i](#openshift-source-to-image)

* [OpenShift & Jenkins - create a CI/CD Pipeline using OpenShift Jenkins Plugin](#jenkins-cicd-pipeline-on-openshift)

## Resources

[Open Liberty image compatible with OpenShift](https://hub.docker.com/r/openliberty/open-liberty-s2i/tags)

[Java Application details created by N. Heidloff ](https://github.com/nheidloff/openshift-on-ibm-cloud-workshops/blob/master/2-deploying-to-openshift/documentation/3-java.md#lab-3---understanding-the-java-implementation)

---

## OpenShift Source to Image

![IBM](../../images/ocp2.png?raw=true "IBM") 

**Steps for building and deploying the application using s2i**

1.  Create a new s2i BuildConfig based on `openliberty/open-liberty-s2i:19.0.0.12`
```
git clone https://github.com/vladsancira/openliberty-tekton.git
cd openliberty-tekton
mvn clean package
oc new-build openliberty/open-liberty-s2i:19.0.0.12 --name=openliberty-app --binary=true --strategy=source 
```

2.  Start the build by passing the current folder as input for the new BuildConfig
```
oc start-build bc/openliberty-app --from-dir=. --wait=true --follow=true
```

3.  Create a new Application based on ImageStreamTag `openliberty-app:latest` from previous step. Then expose an external Route 
```
oc new-app -i openliberty-app:latest
oc expose svc/openliberty-app
oc label dc/openliberty-app app.kubernetes.io/name=java --overwrite
```

4.  Set Readiness, Livness probes  and change deploy strategy to Recreate
```
oc set probe dc/openliberty-app --readiness --get-url=http://:9080/health --initial-delay-seconds=60
oc set probe dc/openliberty-app --liveness --get-url=http://:9080/ --initial-delay-seconds=60
oc patch dc/openliberty-app -p '{"spec":{"strategy":{"type":"Recreate"}}}'
```
FYI : a new deploy will start as DC has an deployconfig change trigger. To check triggers :
```
oc set triggers dc/nodejs-app
```

5.  Open application 
```
oc get route openliberty-app
```

6.  Delete all resources using Labels
```
oc delete all -l build=openliberty-app
oc delete all -l app=openliberty-app
```

---

![IBM](../../images/ocp2.png?raw=true "IBM") ![IBM](./images/jenkins2.jpg?raw=true "IBM")

## Jenkins CI/CD Pipeline on OpenShift 

**Eventhoug Jenking Build Strategy is deprecated in OpenShift 4.3, you can still use the Jenkinsfile inside a Jenkins container.**

**Prerequisites**
----

- Create new CI project `env-ci` and DEV,STAGE projects `env-dev`,`env-stage`
```
oc new-project env-ci
oc new-project env-dev
oc new-project env-stage
```
- Deploy OCP Jenkins template in Project `env-ci`
- Allow `jenkins` ServiceAccount to make deploys on other projects
```

oc policy add-role-to-user edit system:serviceaccount:env-ci:jenkins -n env-dev
oc policy add-role-to-user edit system:serviceaccount:env-ci:jenkins -n env-stage
```

**Steps**
----

1. Create BuildConifg resource in OpenShift : 
```
oc create -f  ci-cd-pipeline/jenkins-openshift/liberty-ci-cd-pipeline.yaml  -n env-ci
```

2. Create Secret for GitHub integration : 
```
oc create secret generic githubkey --from-literal=WebHookSecretKey=5f345f345c345 -n env-ci
```

3. Add WebHook to GitHub from Settings -> WebHook : 

![Webhook](images/webhook.jpg?raw=true "Webhook") 


4. Start pipeline build or push files into GitHub repo : 
```
oc start-build bc/liberty-pipeline-ci-cd -n env-ci
```

5. Get Routes for `liberty-jenkins` Service : 
```
oc get routes/liberty-jenkins -n env-dev
oc get routes/liberty-jenkins -n env-stage
```

6. Inspect build :

![Jenkins](images/jenkins.jpg?raw=true "Jenkins") 
