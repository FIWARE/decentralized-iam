# decentralized-iam

[![](https://nexus.lab.fiware.org/repository/raw/public/badges/chapters/security.svg)](https://github.com/FIWARE/catalogue/tree/master/security/README.md)

The FIWARE Decentralized Identity and Access Management (decentralized-aim) is an integrated suite of components designed to facilitate authentication using Verifiable Credentials (VCs) and authorization based on ODRL policies.

This repository provides a description of the FIWARE decentralized IAM, its technical implementation and deployment recipes.

This project is part of [FIWARE](https://www.fiware.org/). For more information check the FIWARE Catalogue entry for
[Security](https://github.com/FIWARE/catalogue/tree/master/security).

| :books: [Documentation]()  |  :dart: [Roadmap](https://github.com/FIWARE/decentralized-iam/tree/master/doc/ROADMAP.md)|
|---|---|

<details>
<summary><strong>Table of Contents</strong></summary>

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->

**Table of Contents**

- [decentralized-iam](#decentralized-iam)
  - [Overview](#overview)
  - [Release Information](#release-information)
  - [Deployment](#deployment)
    - [Local Deployment](#local-deployment)
    - [Deployment with Helm](#deployment-with-helm)
  - [Testing](#testing)
  - [How to contribute](#how-to-contribute)
  - [License](#license)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Overview
The FIWARE decentralized IAM solution enables secure and decentralized authentication mechanisms by leveraging Verifiable Credentials (VCs) and authorization based on attribute-based access control. More specifically, it allows to:
* Interface with Trust Services aligned with [EBSI specifications](https://api-pilot.ebsi.eu/docs/apis)
* Implement authentication based on [W3C DID](https://www.w3.org/TR/did-core/) with 
  [VC/VP standards](https://www.w3.org/TR/vc-data-model/) and 
  [SIOPv2](https://openid.net/specs/openid-connect-self-issued-v2-1_0.html#name-cross-device-self-issued-op) / 
  [OIDC4VP](https://openid.net/specs/openid-4-verifiable-presentations-1_0.html#request_scope) protocols
* Implement authorization based on attribute-based access control (ABAC) following an 
  [XACML P*P architecture](https://www.oasis-open.org/committees/tc_home.php?wg_abbrev=xacml) using 
  [Open Digital Rights Language (ODRL)](https://www.w3.org/TR/odrl-model/) and the 
  [Open Policy Agent (OPA)](https://www.openpolicyagent.org/)

Technically, the FIWARE decentralized IAM is a 
[Helm Umbrella-Chart](https://helm.sh/docs/howto/charts_tips_and_tricks/#complex-charts-with-many-dependencies), 
containing all the sub-charts and their dependencies for deployment via Helm (i.e. [decentralized-iam](https://github.com/FIWARE/decentralized-iam) and [odrl-authorization](https://github.com/fiware/odrl-authorization) charts).  
Thus, being provided as Helm chart, the FIWARE decentralized IAM can be deployed on 
[Kubernetes](https://kubernetes.io/) environments.

## Release Information

The FIWARE decentralized IAM uses a continious integration flow, where every merge to the main-branch triggers a new release. Versioning follows [Semantic Versioning 2.0.0](https://semver.org/lang/de/), therefor only major changes will contain breaking changes. 
Important releases will be listed below, with additional information linked:

## Deployment

> :warning: The `deploy` directory in the repository contains everything necessary to set up the [local deployment](#local-deployment), with all required dependencies ready for use. However, **the `deploy` directory must not be used as is for deployments in real or production environments**.

> :warning: The passwords used in the `deploy` config dir for the [local deployment](#local-deployment), although they may appear secure, have been provided for the sake of greater reproducibility (to prevent consecutive deployments from modifying the stored and configured credentials, thus avoiding errors). However, **under no circumstances should they be used in real or production environments**.

### Local Deployment

The FIWARE decentralized IAM provides a minimal local deployment setup intended for development and testing purposes.

The requirements for the local deployment are:
* [Maven](https://maven.apache.org/)
* Java Development Kit (at least v17)
* [Docker](https://www.docker.com/)
* [Helm](https://helm.sh/)
* [Helmfile](https://helmfile.readthedocs.io/en/latest/)

In order to interact with the system, the following tools are also helpful:
- [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/)
- [curl](https://curl.se/download.html)
- [jq](https://stedolan.github.io/jq/download/)
- [yq](https://mikefarah.gitbook.io/yq/)

> :warning: In current Linux installations, ```br_netfilter``` is disabled by default. That leads to networking issues inside the k3s cluster and will prevent the connector to start up properly. Make sure that its enabled via ```modprobe br_netfilter```. See [Stackoverflow](https://stackoverflow.com/questions/48148838/kube-dns-error-reply-from-unexpected-source/48151279#48151279) for more.

To start the deployment, just use:

```shell
    mvn clean deploy -Plocal
```

### Deployment with Helm

The decentralized-iam is a [Helm Umbrella-Chart](https://helm.sh/docs/howto/charts_tips_and_tricks/#complex-charts-with-many-dependencies), containing all the sub-charts of the different components and their dependencies. Its sources can be found 
[here](./charts/decentralized-iam).

The chart is available at the repository ```https://fiware.github.io/decentralized-iam/```. You can install it via:

```shell
    # add the repo
    helm repo add decentralized-iam https://fiware.github.io/decentralized-iam/
    # install the chart
    helm install <DeploymentName> decentralized-iam/decentralized-iam -n <Namespace> -f values.yaml
```

**Note,** that due to the app-of-apps structure of the deployment and the different dependencies between the components, a deployment without providing any configuration values will not work. Make sure to provide a 
`values.yaml` file for the deployment, specifying all necessary parameters. This includes setting parameters of the endpoints, DNS information (providing Ingress or OpenShift Route parameters), 
structure and type of the required VCs, internal hostnames of the different components and providing the configuration of the DID and keys/certs.

Configurations for all sub-charts (and sub-dependencies) can be managed through the top-level [values.yaml](./charts/decentralized-iam/values.yaml) of the chart. It contains the default values of each component and additional parameter shared between the components. The configuration of the applications can be changed under the key ```<APPLICATION_NAME>```, please see the individual applications and there sub-charts for the available options.

The chart is [published and released](./github/workflows/release-helm.yaml) on each merge to master.

## Testing

In order to test the [helm-chart](./charts/decentralized-iam) provided for the FIWARE decentralized IAM, an integration-test 
framework based on [Cucumber](https://cucumber.io/) and [Junit5](https://junit.org/junit5/) is provided: [it](./it).

The tests can be executed via: 
```shell
    mvn clean integration-test -Ptest
```
They will spin up the [Local Deployment](#local-deployment) and run 
the [test-scenarios](./it/src/test/resources/it/mvds_basic.feature) against it.

## How to contribute

Please, check the doc [here](doc/CONTRIBUTING.md).

## License
FIWARE decentralized-iam is licensed under [Apache 2.0 License](LICENSE).

For the avoidance of doubt, the owners of this software
wish to make a clarifying public statement as follows:

> Please note that software derived as a result of modifying the source code of this
> software in order to fix a bug or incorporate enhancements is considered a derivative 
> work of the product. Software that merely uses or aggregates (i.e. links to) an otherwise 
> unmodified version of existing software is not considered a derivative work, and therefore
> it does not need to be released as under the same license, or even released as open source.