# OpenShift and K8S on IBM Cloud with Tekton and Jenkins

![IBM](./images/os-logo.jpg?raw=true "IBM")

[Red Hat OpenShift on IBM Cloud](https://cloud.ibm.com/docs/openshift?topic=openshift-why_openshift) is an extension of the IBM Cloud Kubernetes Service, where IBM manages OpenShift Container Platform for you. 

With Red Hat OpenShift on IBM Cloud developers have a fast and secure way to containerize and deploy enterprise workloads in Kubernetes clusters. OpenShift clusters build on Kubernetes container orchestration that offers consistency and flexibility for your development lifecycle operations.

This repository holds a series of tutorials that help you as a developer to become familiar with Cloud-native Continuous Integration / Continuous Delivery pipelines, Git webhooks, builds and deployments on Red Hat OpenShift 4.3 and K8S 1.16+ using Tekton Pipelines.

In order to run these tutorials, you need an [IBM Cloud account](https://cloud.ibm.com/registration).

IBM Cloud offers a free Kubernetes 1.16 cluster for 1 month for testing purposes and a free of license fee Red Hat OpenShift 4.3.5 beta cluster. Also, you recieve by default a free IBM Cloud Image Registry with 512MB storage and 5GB Pull Trafic each month. 

## Deploy OpenLiberty Application using Tekton Pipelines

**Tutorials**

* [Create a Cloud-native CI/CD Pipeline on OpenShift 4.3](#1-cloud-native-cicd-pipeline-on-openshift)

* [Create a Cloud-native CI/CD Pipeline on Kubernetes 1.16+](#2-cloud-native-cicd-pipeline-on-kubernetes)

* [Create a WebHook connection from Git to our CI/CD Pipeline](#3-create-a-webhook-connection)

* [OpenShift & S2I (Source to Image) - build and deploy an OpenLiberty application](#4-openshift-source-to-image)

* [Create a Jenkins CI/CD Pipeline on OpenShift 4.2](#5-deprecated--jenkins-cicd-pipeline-on-openshift)


**Resources**

[Open Liberty image compatible with OpenShift](https://hub.docker.com/r/openliberty/open-liberty-s2i/tags)

[Java Application details created by N. Heidloff ](https://github.com/nheidloff/openshift-on-ibm-cloud-workshops/blob/master/2-deploying-to-openshift/documentation/3-java.md#lab-3---understanding-the-java-implementation)


**Repository Content**

* `.s2i/bin`                folder contains custom s2i scripts for assembling and running the application image for OpenShift v4.

* `.m2/settings.xml`        custom Maven settings.xml config file (optional).

* `liberty-config`          folder contains the Open Liberty server.xml config file that will be copied to OpenLiberty image.

* `ci-cd-pipeline   `       folder contains pipeline implementation for different targets.

* `tekton-openshift `       folder contains the [OpenShift Pipelines](https://www.openshift.com/learn/topics/pipelines) implementation and yamls.

* `tekton-kubernetes`       folder contains the [Kubernetes Pipelines](https://github.com/tektoncd/pipeline) implementation and yaml.

* `tekton-triggers  `       folder contains the implementation for [Tekton Triggers](https://github.com/tektoncd/triggers) for creating a Git WebHook.

* `jenkins-openshift`       folder contains the Jenkins Pipeline implementation (Jenkinsfile) and yaml for creating the BuildConfig with pipeline strategy.
---

![IBM](images/ocp2.png?raw=true "IBM") ![IBM](images/tekton2.jpg?raw=true "IBM")

## 1. Cloud native CI/CD Pipeline on OpenShift

**Prerequisites**
----
 
- Install OpenShift Pipeline Operator
- Create CI and DEV Projects
```
oc new-project env-ci
oc new-project env-dev
oc new-project env-stage
```  
- Create Image Stream `nodejs-tekton` for storing NodeJS image
```
oc create is liberty-tekton -n env-dev
oc create is liberty-tekton -n env-stage
``` 
- Allow pipeline SA to make deploys on other projects
```
oc adm policy add-scc-to-user privileged system:serviceaccount:env-ci:pipeline -n env-ci
oc adm policy add-scc-to-user privileged system:serviceaccount:env-ci:pipeline -n env-dev
oc adm policy add-scc-to-user privileged system:serviceaccount:env-ci:pipeline -n env-stage
oc adm policy add-role-to-user edit system:serviceaccount:env-ci:pipeline -n env-ci
oc adm policy add-role-to-user edit system:serviceaccount:env-ci:pipeline -n env-dev
oc adm policy add-role-to-user edit system:serviceaccount:env-ci:pipeline -n env-stage
```

**Pipeline design**
----

![Pipeline Design](images/pipeline-design-openshift-simple.jpg?raw=true "Pipeline Design")


**Steps for creating the Continuous Integration - Continuous Delivery Pipeline**
----

0. clone git project
```
git clone https://github.com/vladsancira/openliberty-tekton.git
cd openliberty-tekton
```

1. create Tekton resources , taks and pipeline
```
oc create -f ci-cd-pipeline/tekton-openshift/resources.yaml        -n env-ci
oc create -f ci-cd-pipeline/tekton-openshift/task-build-s2i.yaml   -n env-ci
oc create -f ci-cd-pipeline/tekton-openshift/task-deploy.yaml      -n env-ci
oc create -f ci-cd-pipeline/tekton-openshift/task-test.yaml        -n env-ci
oc create -f ci-cd-pipeline/tekton-openshift/task-promote.yaml     -n env-ci
oc create -f ci-cd-pipeline/tekton-openshift/pipeline.yaml         -n env-ci
```

2. execute pipeline
```
tkn t ls -n env-ci
tkn p ls -n env-ci
tkn p start liberty-pipeline -n env-ci
```

![Pipeline Run](images/pipeline.jpg?raw=true "Pipeline Run") 

---

![IBM](./images/k8s.png?raw=true "IBM") ![IBM](images/tekton2.jpg?raw=true "IBM")

## 2. Cloud native CI/CD Pipeline on Kubernetes

**Prerequisites**
----

- Clone git project
```
git clone https://github.com/vladsancira/openliberty-tekton.git
cd nodejs-tekton
```

- Install Tekton pipelines in default `tekton-pipelines` namespace :
```
kubectl apply --filename https://storage.googleapis.com/tekton-releases/latest/release.yaml
kubectl get pods --namespace tekton-pipelines
```

- Create new `env-dev` and `env-ci` namespaces :
```
kubectl create namespace env-stage
kubectl create namespace env-dev
kubectl create namespace env-ci
```

- Create <API_KEY> for IBM Cloud Registry and export PullImage secret from `default` namespace :
```
ibmcloud iam api-key-create MyKey -d "this is my API key" --file key_file.json
cat key_file.json | grep apikey

kubectl create secret generic ibm-cr-secret  -n env-ci --type="kubernetes.io/basic-auth" --from-literal=username=iamapikey --from-literal=password=<API_KEY>
kubectl annotate secret ibm-cr-secret  -n env-ci tekton.dev/docker-0=us.icr.io

kubectl get secret default-us-icr-io --export -o yaml > default-us-icr-io.yaml
kubectl create -f  default-us-icr-io.yaml -n env-dev
kubectl create -f  default-us-icr-io.yaml -n env-stage
```

- Create Service Account to allow pipeline to run and deploy to `env-dev` namespace :
```
kubectl apply -f ci-cd-pipeline/tekton-kubernetes/service-account.yaml         -n env-ci
kubectl apply -f ci-cd-pipeline/tekton-kubernetes/service-account-binding.yaml -n env-dev
kubectl apply -f ci-cd-pipeline/tekton-kubernetes/service-account-binding.yaml -n env-stage
```

**Pipeline design**
----

![Pipeline Design](images/pipeline-design-tekton-simple.jpg?raw=true "Pipeline Design")

**Steps for creating the CI-CD pipeline**
----

1. create Tekton resources , taks and pipeline:
```
kubectl create -f ci-cd-pipeline/tekton-kubernetes/resources.yaml          -n env-ci
kubectl create -f ci-cd-pipeline/tekton-kubernetes/task-build.yaml         -n env-ci
kubectl create -f ci-cd-pipeline/tekton-kubernetes/task-deploy.yaml        -n env-ci
kubectl create -f ci-cd-pipeline/tekton-kubernetes/task-test.yaml          -n env-ci
kubectl create -f ci-cd-pipeline/tekton-kubernetes/task-promote.yaml       -n env-ci
kubectl create -f ci-cd-pipeline/tekton-kubernetes/pipeline.yaml           -n env-ci
```

2. execute pipeline via Pipeline Run and watch :
```
kubectl create -f ci-cd-pipeline/tekton-kubernetes/pipeline-run.yaml -n env-ci
kubectl get pipelinerun -n env-ci -w
```

3. check pods and logs :
```
kubectl get pods                             -n env-dev
kubectl get pods                             -n env-stage
kubectl logs liberty-app-76fcdc6759-pjxs7 -f -n env-dev
```

4. open browser with cluster IP and port 32427 :
get Cluster Public IP :
```
kubectl get nodes -o wide
```

http://<CLUSTER_IP>>:32427/health

---


## 3. Create a WebHook connection


In order to create a webhook from Git to our Tekton Pipeline we need to install [TektonCD Triggers](https://github.com/tektoncd/triggers) in our K8s cluster. 
Triggers is a Kubernetes Custom Resource Defintion (CRD) controller that allows you to extract information from events payloads (a "trigger") to create Kubernetes resources.
More information can be found in the  [TektonCD Triggers Project](https://github.com/tektoncd/triggers). Also we can use Tekton Dashboard as a web console for viewing all Tekton Resources. 

On OpenShift 4.3 , [TektonCD Triggers](https://github.com/tektoncd/triggers) are already installed as part of the [OpenShift Pipelines Operator](https://www.openshift.com/learn/topics/pipelines),  in `openshift-pipelines` project (namespace), but Tekton Dashboard is not installed. Instead,  we can use the OpenShift Pipeline Web Console.

The mechanism for triggering builds via WebHooks is the same and involves creating an EventListener and exposing that EventListener Service to outside.

![Tekton Architecture](./images/webhook-architecture-tekton-simple.jpg?raw=true "Tekton Architecture")


**For OpenShift we need to**
----

* create Pipeline's trigger_template, trigger_binding & event_listener

```
oc create -f ci-cd-pipeline/tekton-triggers/webhook-event-listener-openshift.yaml -n env-ci 
```
* create a Route for the event_listener service
```
oc expose svc/el-liberty-pipeline-listener -n env-ci
oc get route -n env-ci
```
*  add this route to out Git WebHook


**For Kubernetes we need to**  
----

0. Install Tekton Dashboard and Tekton Triggers
```
kubectl apply -f https://github.com/tektoncd/dashboard/releases/download/v0.5.3/tekton-dashboard-release.yaml
kubectl apply -f https://storage.googleapis.com/tekton-releases/triggers/latest/release.yaml
kubectl apply -f ci-cd-pipeline/tekton-triggers/tekton-dashboard.yaml -n tekton-pipelines
```

1. Create ServiceAccount, Role and RoleBinding 
```
kubectl apply  -f ci-cd-pipeline/tekton-triggers/webhook-service-account.yaml  -n env-ci
```

2. Create Pipeline's trigger_template, trigger_binding & event_listener<br>
**by default Event Listener service type is ClusterIP , but we set it to NodePort so it can be triggered from outside cluster**

```
kubectl apply -f ci-cd-pipeline/tekton-triggers/webhook-event-listener-kubernetes.yaml -n env-ci 
```

3. Get el-nodejs-pipeline-listener PORT and cluster EXTERNAL-IP
```
kubectl get svc el-liberty-pipeline-listener -n env-ci
kubectl get nodes -o wide 
``` 

4. Add 'http://<CLUSTER_IP>>:<EVENT_LISTNER_PORT>' to GitHib as WebHook. Then perform a push.

![Webhook](./images/webhook-tekton.jpg?raw=true "Webhook") 


5. Open Tekton Dashboard  :  http://<CLUSTER_IP>>:32428/#/pipelineruns

![Webhook](./images/dashboard.jpg?raw=true "Webhook") 


---

![IBM](images/ocp2.png?raw=true "IBM") 

## 4. OpenShift source to image

**Steps for creating the Pipeline and WebHook**

1.  Delete all resources
```
oc delete all -l build=openliberty-app
oc delete all -l app=openliberty-app
```

2.  Create new s2i build config based on openliberty/open-liberty-s2i:19.0.0.12 and imagestream
```
git clone https://github.com/vladsancira/openliberty-tekton.git
cd openliberty-tekton
mvn clean package
oc new-build openliberty/open-liberty-s2i:19.0.0.12 --name=openliberty-app --binary=true --strategy=source 
```

3.  Create application image from srouce
```
oc start-build bc/openliberty-app --from-dir=. --wait=true --follow=true
```

4.  Create application based on imagestreamtag : openliberty-app:latest
```
oc new-app -i openliberty-app:latest
oc expose svc/openliberty-app
oc label dc/openliberty-app app.kubernetes.io/name=java --overwrite
```

5.  Set readiness and livness probes , and change deploy strategy to Recreate
```
oc set probe dc/openliberty-app --readiness --get-url=http://:9080/health --initial-delay-seconds=60
oc set probe dc/openliberty-app --liveness --get-url=http://:9080/ --initial-delay-seconds=60
oc patch dc/openliberty-app -p '{"spec":{"strategy":{"type":"Recreate"}}}'
```
FYI : a new deploy will start as DC has an deployconfig change trigger. To check triggers :
```
oc set triggers dc/nodejs-app
```

6.  Open application 
```
oc get route openliberty-app
```

---

![IBM](images/ocp2.png?raw=true "IBM") ![IBM](images/jenkins2.jpg?raw=true "IBM")

## 5. DEPRECATED : Jenkins CI/CD Pipeline on OpenShift 

**Prerequisites**

- Create new CI project `env-ci` and DEV project `env-dev`
```
oc new-project env-ci
oc new-project env-dev
```
- Deploy OCP Jenkins template in project `env-ci`
- Allow jenkins SA to make deploys on other projects
```
oc policy add-role-to-user edit system:serviceaccount:env-ci:jenkins -n env-dev
```

**Steps**

1. create BuildConifg resource in OpenShift : 
```
oc create -f  ci-cd-pipeline/jenkins-openshift/liberty-ci-cd-pipeline.yaml  -n env-ci
```

2. create secret for GitHub integration : 
```
oc create secret generic githubkey --from-literal=WebHookSecretKey=5f345f345c345 -n env-ci
```

3. add WebHook to GitHub from Settings -> WebHook : 

![Webhook](images/webhook.jpg?raw=true "Webhook") 


4. start pipeline build or push files into GitHub repo : 
```
oc start-build bc/liberty-pipeline-ci-cd -n env-ci
```

5. get routes for simple-nodejs-app : 
```
oc get routes/liberty-jenkins -n env-dev
oc get routes/liberty-jenkins -n env-stage
```

6. inspect build :

![Jenkins](images/jenkins.jpg?raw=true "Jenkins") 
