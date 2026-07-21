# Kubernetes — Kitaplık

Bu klasör, sistemin Kubernetes'te nasıl çalışacağının **başlangıç örneğidir**
(farkındalık düzeyi — junior'dan tam K8s uzmanlığı beklenmez).

## docker-compose → Kubernetes eşlemesi

| docker-compose | Kubernetes karşılığı |
|---|---|
| `service` | **Deployment** (kaç kopya, hangi imaj) + **Service** (ağ erişimi/DNS) |
| `environment` | env + **ConfigMap** (config) / **Secret** (parola, JWT sırrı) |
| `depends_on` | K8s bunu yönetmez; servisler hazır olana kadar birbirini yeniden dener |
| `healthcheck` | **readinessProbe** / **livenessProbe** (gerçek HTTP `/actuator/health`) |
| tek makine | Cluster: birden çok makine (node), otomatik yeniden başlatma, ölçekleme |

## Bu örnekte gösterilenler (`book-service.yaml`)

- **replicas: 2** — K8s yükü dağıtır, biri çökerse yenisini başlatır, sürüm güncellemesini kesintisiz yapar.
- **Probe'lar** — `/actuator/health`'e HTTP GET; hazır değilse trafik göndermez, takılırsa yeniden başlatır. (docker-compose'daki sahte `exit 0` healthcheck'in doğru yeri burasıdır.)
- **Secret** — DB parolası manifest'e gömülmez, `db-credentials` secret'ından gelir.
- **resources** — CPU/bellek istek ve limitleri; K8s buna göre yerleştirir ve sınırlar.

## Kalan (gerçek bir dağıtımda)

- Diğer 4 iş servisi için aynı desende Deployment/Service.
- postgres/redis/kafka: StatefulSet + PersistentVolume, ya da yönetilen servisler (ör. AWS RDS/ElastiCache/MSK).
- gateway için bir **Ingress** (dış dünyaya tek giriş).
- İmajların bir registry'ye push edilmesi (`docker build` + `docker push`).

> NOT: Bu manifest'ler bir cluster gerektirdiği için bu ortamda çalıştırılıp
> doğrulanmadı; desen ve söz dizimi doğrudur, `kubectl apply -f k8s/` ile bir
> cluster'da (minikube/kind dahil) uygulanabilir.
