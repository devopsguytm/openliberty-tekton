# OpenShift, K8S and Tekton on IBM Cloud

![IBM](./images/os-logo.jpg?raw=true "IBM")

[Red Hat OpenShift on IBM Cloud](https://cloud.ibm.com/docs/openshift?topic=openshift-why_openshift) is an extension of the IBM Cloud Kubernetes Service, where IBM manages OpenShift Container Platform for you. 

With Red Hat OpenShift on IBM Cloud developers have a fast and secure way to containerize and deploy enterprise workloads in Kubernetes clusters. OpenShift clusters build on Kubernetes container orchestration that offers consistency and flexibility for your development lifecycle operations.

This repository holds a series of tutorials that help you as a developer to become familiar with builds, deployments and webhooks on Red Hat OpenShift 4.3 and K8S 1.16+ in IBM Cloud using Tekton Pipelines. 

IBM Cloud offers a free Kubernetes 1.16 cluster for 1 month for testing purposes and a free of license fee Red Hat OpenShift 4.3.5 beta cluster. Also, you recieve by default a free IBM Cloud Image Registry with 512MB storage and 5GB Pull Trafic each month. 

In order to run these tutorials, you need an [IBM Cloud account](https://cloud.ibm.com/registration).

---

## Deploy OpenLiberty Application using Tekton Pipelines

**Resources**

* [S2I Build Task from OpenShift catalog](https://github.com/openshift/pipelines-catalog)

* [Kaniko Build Taks from Tekton Catalog](https://github.com/tektoncd/catalog/tree/master/kaniko)

* [Open Liberty Image compatible with OpenShift](https://hub.docker.com/r/openliberty/open-liberty-s2i/tags)

* [Java Application created by N. Heidloff ](https://github.com/nheidloff/openshift-on-ibm-cloud-workshops/blob/master/2-deploying-to-openshift/documentation/3-java.md#lab-3---understanding-the-java-implementation)


**Content**

* `.s2i/bin`               folder contains custom s2i scripts for assembling and running the application image for OpenShift v4.

* `.m2/settings.xml`       custom Maven settings.xml config file (optional).

* `liberty-config`         folder contains the Open Liberty server.xml config file that will be copied to OpenLiberty image.

* `ci-cd-pipeline`          folder contains pipelines implementation for different targets.

* `openshift-tekton`       folder contains the yamls for creating the Tekton Pipeline, Tasks and Resources in OpenShift 4.3

* `kubernetes-tekton`      folder contains the yamls for creating the Tekton Pipeline, Tasks and Resources in Kubernetes 1.16

* `openshift-jenkins`      folder contains the Jenkins pipeline implementation and yaml for creating the OpenShift BuildConfig with Jenkins Pipeline strategy.

---

![IBM](images/ocp2.png?raw=true "IBM") ![IBM](images/tekton2.jpg?raw=true "IBM")

## Continuous Integration - Continuous Delivery with Tekton Pipelines 

**Prerequisites**
 
- Install OpenShift Pipeline Operator
- Create `env-ci` and `env-dev` Projects
```
oc new-project env-ci
oc new-project env-dev
```  
- Create ImageStream `nodejs-tekton` for storing NodeJS Image
```
oc create is nodejs-tekton -n env-dev
``` 
- Allow `pipeline` ServiceAccount to make deploys on other projects
```
oc create serviceaccount pipeline -n env-ci
oc adm policy add-scc-to-user privileged system:serviceaccount:env-ci:pipeline -n env-ci
oc adm policy add-scc-to-user privileged system:serviceaccount:env-ci:pipeline -n env-dev
oc adm policy add-role-to-user edit system:serviceaccount:env-ci:pipeline -n env-ci
oc adm policy add-role-to-user edit system:serviceaccount:env-ci:pipeline -n env-dev
```

**Steps for creating the CI-CD pipeline**

0. Clone Git project
```
git clone https://github.com/vladsancira/openliberty-tekton.git
cd nodejs-tekton
```

1. Create Tekton Resources , Taks and Pipeline
```
oc create -f ci-cd-pipeline/openshift-tekton/resources.yaml        -n env-ci
oc create -f ci-cd-pipeline/openshift-tekton/task-build-s2i.yaml   -n env-ci
oc create -f ci-cd-pipeline/openshift-tekton/task-test.yaml        -n env-ci
oc create -f ci-cd-pipeline/openshift-tekton/task-deploy.yaml      -n env-ci
oc create -f ci-cd-pipeline/openshift-tekton/pipeline.yaml         -n env-ci
```

2. Execute Pipeline
```
tkn t ls -n env-ci
tkn p ls -n env-ci
tkn p start liberty-pipeline -n env-ci
```

![Pipeline Run](images/pipeline.jpg?raw=true "Pipeline Run") 

---

![IBM](./images/k8s.png?raw=true "IBM") ![IBM](images/tekton2.jpg?raw=true "IBM")
## Continuous Integration - Continuous Delivery with Tekton Pipelines 

**Prerequisites**

- Clone Git project
```
git clone https://github.com/vladsancira/openliberty-tekton.git
cd nodejs-tekton
```

- Install Tekton Pipelines in default `tekton-pipelines` Namespace :
```
kubectl apply --filename https://storage.googleapis.com/tekton-releases/latest/release.yaml
kubectl get pods --namespace tekton-pipelines
```

- Create new `env-dev` and `env-ci` Namespaces :
```
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
```

- Create Service Account to allow pipeline to run and deploy to `env-dev` namespace :
```
kubectl apply -f ci-cd-pipeline/kubernetes-tekton/service-account.yaml         -n env-ci
kubectl apply -f ci-cd-pipeline/kubernetes-tekton/service-account-binding.yaml -n env-dev
```

**Steps for creating the CI-CD pipeline**


1. Create Tekton Resources , Taks and Pipeline:
```
kubectl create -f ci-cd-pipeline/kubernetes-tekton/resources.yaml   -n env-ci
kubectl create -f ci-cd-pipeline/kubernetes-tekton/task-build.yaml  -n env-ci
kubectl create -f ci-cd-pipeline/kubernetes-tekton/task-deploy.yaml -n env-ci
kubectl create -f ci-cd-pipeline/kubernetes-tekton/pipeline.yaml    -n env-ci
```

2. Execute Pipeline via PipelineRun and watch :
```
kubectl create -f ci-cd-pipeline/kubernetes-tekton/pipeline-run.yaml -n env-ci
kubectl get pipelinerun -n env-ci -w
```

3. Check Pods and logs :
```
kubectl get pods                             -n env-dev
kubectl logs liberty-app-76fcdc6759-pjxs7 -f -n env-dev
```

4. Open Browser with cluster IP and port 32427 :
get Cluster Public IP :
```
kubectl get nodes -o wide
```

http://<CLUSTER_IP>>:32427/health

---


**Create Tekton Pipeline WebHooks for Git - Architecture**

![Tekton Architecture](./images/architecture.jpg?raw=true "Tekton Architecture")

[https://github.com/tektoncd/triggers/blob/master/docs/triggerbindings.md](https://github.com/tektoncd/triggers/blob/master/docs/triggerbindings.md)<br>
[https://github.com/tektoncd/triggers/blob/master/docs/triggertemplates.md](https://github.com/tektoncd/triggers/blob/master/docs/triggertemplates.md)<br>
[https://github.com/tektoncd/triggers/blob/master/docs/eventlisteners.md](https://github.com/tektoncd/triggers/blob/master/docs/eventlisteners.md)<br>


**Prerequisites**

- Install Tekton Dashboard and Tekton Triggers
```
kubectl apply -f https://github.com/tektoncd/dashboard/releases/download/v0.5.3/tekton-dashboard-release.yaml
kubectl apply -f https://storage.googleapis.com/tekton-releases/triggers/latest/release.yaml
kubectl apply -f ci-cd-pipeline/kubernetes-tekton/tekton-dashboard.yaml -n tekton-pipelines
```

**Steps for creating the WebHook**

1. Create ServiceAccount, Role and RoleBinding 
```
kubectl apply  -f ci-cd-pipeline/kubernetes-tekton/webhook-service-account.yaml  -n env-ci
```

2. Create Pipeline's trigger_template, trigger_binding & event_listener<br>
**by default Event Listener service type is ClusterIP , but we set it to NodePort so it can be triggered from outside cluster**

```
kubectl apply -f ci-cd-pipeline/kubernetes-tekton/webhook-event-listener.yaml -n env-ci 
```

3. Get el-nodejs-pipeline-listener PORT and cluster EXTERNAL-IP
```
kubectl get svc el-liberty-pipeline-listener -n env-ci
kubectl get nodes -o wide 
``` 

4. Add 'http://<CLUSTER_IP>>:<EVENT_LISTNER_PORT>' to GitHib as WebHook. Then perform a push

![Webhook](./images/webhook-tekton.jpg?raw=true "Webhook") 


5. Open Tekton Dashboard  :  http://<CLUSTER_IP>>:32428/#/pipelineruns

![Webhook](./images/dashboard.jpg?raw=true "Webhook") 


---
![IBM](images/ocp2.png?raw=true "IBM") 
## Create application image using S2I (source to image) and deploy it 

**Steps for creating the Pipeline and WebHook**

1.  Delete all resources
```
oc delete all -l build=openliberty-app
oc delete all -l app=openliberty-app
```

2.  Create new s2i BuildConfig based on openliberty/open-liberty-s2i:19.0.0.12 and ImageStream
```
git clone https://github.com/vladsancira/openliberty-tekton.git
cd openliberty-tekton
mvn clean package
oc new-build openliberty/open-liberty-s2i:19.0.0.12 --name=openliberty-app --binary=true --strategy=source 
```

3.  Create application Image from source
```
oc start-build bc/openliberty-app --from-dir=. --wait=true --follow=true
```

4.  Create application based on ImageStreamTag : openliberty-app:latest
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
FYI : a new deploy will start as the DeploymentConfig has a change trigger set by default. To check DeploymentConfig triggers :
```
oc set triggers dc/nodejs-app
```

6.  Open application url
```
oc get route openliberty-app
```

---

![IBM](images/ocp2.png?raw=true "IBM") ![IBM](images/jenkins2.jpg?raw=true "IBM")
## DEPRECATED : Continuous Integration - Continuous Delivery with Jenkins Pipelines 

**You can still use the Jenkinsfile inside a Jenkins container.**

**Prerequisites**

- Create new CI project `env-ci` and DEV project `env-dev`
```
oc new-project env-ci
oc new-project env-dev
```
- Deploy OCP Jenkins template in project `env-ci`
- Allow `jenkins` ServiceAccount to make deploys on other Projects
```
oc policy add-role-to-user edit system:serviceaccount:env-ci:jenkins -n env-dev
```

**Steps**

1. Create BuildConifg resource in OpenShift : 
```
oc create -f  ci-cd-pipeline/openshift-jenkins/liberty-ci-cd-pipeline.yaml  -n env-ci
```

2. Create secret for GitHub integration : 
```
oc create secret generic githubkey --from-literal=WebHookSecretKey=5f345f345c345 -n env-ci
```

3. Add WebHook to GitHub from Settings -> WebHook : 

![Webhook](images/webhook.jpg?raw=true "Webhook") 


4. Start pipeline build or push files into GitHub repo : 
```
oc start-build bc/liberty-pipeline-ci-cd -n env-ci
```

5. Get Route for liberty-jenkins : 
```
oc get routes/liberty-jenkins -n env-dev
```

6. Inspect build :

![Jenkins](images/jenkins.jpg?raw=true "Jenkins") 
