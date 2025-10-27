# Export des prix moyens pondérés de SAP vers Coptis

Les scripts de ce projet peuvent être utilisés pour exporter les prix moyens pondérés de SAP
vers Coptis.

Le répertoire K8S contient un job pour automatiser l'exécution de ces scripts sur Kubernetes.

## Déploiement sur K8S

```sh
git clone https://github.com/marvinpac-it/prices-sap-to-coptis.git
cd prices-sap-to-coptis/k8s
kubectl apply -k .
```

### Remplacement du token github

> [!WARNING]
> Lors du remplacement du token github, supprimer le PVC `coptis-prices-shared-pvc`

### Vérification du cronjob

**Lister les jobs planifiés**
```sh
kubectl -n coptis-prices get cronjobs.batch
```

**Résultat**
```sh
NAME                       SCHEDULE     SUSPEND   ACTIVE   LAST SCHEDULE   AGE
sap-to-coptis-prices-job   0 19 * * *   False     0        <none>          20m
```

### Forcer l'exécution d'un job sans attendre l'exécution planifiée

```sh
k create job --from=cronjob/sap-to-coptis-prices-job test-job-1
```

### Vérification des jobs

**Lister les jobs**
```sh
kubectl -n coptis-prices get jobs.batch
```

**Résultat**
```sh
NAME         COMPLETIONS   DURATION   AGE
test-job-1   1/1           6m7s       22m
test-job-2   1/1           53s        15m
```

### Résolution d'erreurs

**Description d'un job**
```sh
k describe -n coptis-prices job/test-job-1
```

**Résultat**
```sh
Name:             test-job-1
Namespace:        coptis-prices
Selector:         batch.kubernetes.io/controller-uid=76c4b8e5-4e3a-4eaf-9825-f762fa4927ad
Labels:           batch.kubernetes.io/controller-uid=76c4b8e5-4e3a-4eaf-9825-f762fa4927ad
                  batch.kubernetes.io/job-name=test-job-1
                  controller-uid=76c4b8e5-4e3a-4eaf-9825-f762fa4927ad
                  job-name=test-job-1
Annotations:      cronjob.kubernetes.io/instantiate: manual
Controlled By:    CronJob/sap-to-coptis-prices-job
Parallelism:      1
Completions:      1
Completion Mode:  NonIndexed
Start Time:       Fri, 30 Aug 2024 10:14:32 +0000
Completed At:     Fri, 30 Aug 2024 10:20:39 +0000
Duration:         6m7s
Pods Statuses:    0 Active (0 Ready) / 1 Succeeded / 0 Failed
Pod Template:
  Labels:  batch.kubernetes.io/controller-uid=76c4b8e5-4e3a-4eaf-9825-f762fa4927ad
           batch.kubernetes.io/job-name=test-job-1
           controller-uid=76c4b8e5-4e3a-4eaf-9825-f762fa4927ad
           job-name=test-job-1
  Init Containers:
   git:
    Image:      alpine/git
    Port:       <none>
    Host Port:  <none>
    Command:
      /bin/sh
      -c
    Args:
      if [ -d "/data/prices-sap-to-coptis/.git" ]; then cd /data/prices-sap-to-coptis && git pull; else git clone --single-branch --depth 1 https://$(GITHUB_TOKEN)@github.com/marvinpac-it/prices-sap-to-coptis.git /data/prices-sap-to-coptis; fi;
    Environment:
      GITHUB_TOKEN:  <set to the key 'token' in secret 'github-token'>  Optional: false
    Mounts:
      /data from coptis-prices-shared-storage (rw)
   groovy:
    Image:      groovy:latest
    Port:       <none>
    Host Port:  <none>
    Command:
      /bin/sh
      -c
    Args:
      cd /data/prices-sap-to-coptis;
      cp /dest/MVP.jcoDestination .
      groovy -cp sapjco3.jar readTableDsl.groovy;

    Environment:  <none>
    Mounts:
      /data from coptis-prices-shared-storage (rw)
      /dest/ from sap-secret-volume (rw)
   r-base:
    Image:      rocker/r-base:latest
    Port:       <none>
    Host Port:  <none>
    Command:
      /bin/sh
      -c
    Args:
      cd /data/prices-sap-to-coptis;
      Rscript AssembleOutput.R;

    Environment:  <none>
    Mounts:
      /data from coptis-prices-shared-storage (rw)
      /usr/local/lib/R/site-library from r-site-library-storage (rw)
  Containers:
   smb-client:
    Image:      ubuntu:latest
    Port:       <none>
    Host Port:  <none>
    Command:
      /bin/sh
      -c
    Args:
      apt update && DEBIAN_FRONTEND=noninteractive TZ=Etc/UTC apt-get -y install smbclient
      cd /data
      smbclient //themis.sce.local/SAP -c 'put final_output.csv; put codes_article.csv' --user=SCE/$(SMB_USER)%$(SMB_PASS)

    Environment:
      SMB_USER:  <set to the key 'username' in secret 'smb-credentials'>  Optional: false
      SMB_PASS:  <set to the key 'password' in secret 'smb-credentials'>  Optional: false
    Mounts:
      /data from coptis-prices-shared-storage (rw)
  Volumes:
   coptis-prices-shared-storage:
    Type:       PersistentVolumeClaim (a reference to a PersistentVolumeClaim in the same namespace)
    ClaimName:  coptis-prices-shared-pvc
    ReadOnly:   false
   sap-secret-volume:
    Type:        Secret (a volume populated by a Secret)
    SecretName:  sap-destination-secret
    Optional:    false
   r-site-library-storage:
    Type:       PersistentVolumeClaim (a reference to a PersistentVolumeClaim in the same namespace)
    ClaimName:  r-site-library-pvc
    ReadOnly:   false
Events:
  Type    Reason            Age   From            Message
  ----    ------            ----  ----            -------
  Normal  SuccessfulCreate  24m   job-controller  Created pod: test-job-1-x9fhc
  Normal  Completed         18m   job-controller  Job completed
```

**Affichage des logs d'un Pod**
```sh
kubectl -n coptis-prices logs test-job-1-x9fhc | tail
```

**Résultat**
```sh
Defaulted container "smb-client" out of: smb-client, git (init), groovy (init), r-base (init)
Setting up python3-samba (2:4.19.5+dfsg-4ubuntu9) ...
Setting up samba-common-bin (2:4.19.5+dfsg-4ubuntu9) ...
Processing triggers for libc-bin (2.39-0ubuntu8.2) ...
Processing triggers for ca-certificates (20240203) ...
Updating certificates in /etc/ssl/certs...
0 added, 0 removed; done.
Running hooks in /etc/ca-certificates/update.d...
done.
putting file final_output.csv as \final_output.csv (185.0 kb/s) (average 185.0 kb/s)
putting file codes_article.csv as \codes_article.csv (242.7 kb/s) (average 214.5 kb/s)
```

**Affichage des logs d'un conteneurs particulier d'un Pod**
Attention, les pods contenant plusieurs conteneurs vont afficher le conteneur par défaut.
Pour afficher le log des autres conteneurs il faut spécifier le conteneur avec `-c <conteneur>`.
Comme on le voit dans la première ligne du résultat précédent, la command logs à pris le conteneur
par défaut `smb-client`. Les autres conteneurs possibles sont listés.

```sh
kubectl -n coptis-prices logs test-job-1-x9fhc -c r-base | tail
```

**Résultat**
```sh
Rows: 1574 Columns: 3
── Column specification ────────────────────────────────────────────────────────
Delimiter: "\t"
chr (1): MATNR
dbl (2): VERPR, PEINH

ℹ Use `spec()` to retrieve the full column specification for this data.
ℹ Specify the column types or set `show_col_types = FALSE` to quiet this message.
Joining with `by = join_by(MATNR)`
Joining with `by = join_by(MATNR)`

```
