# cert-manager Configuration

Let's Encrypt TLS 인증서 자동 발급/갱신을 위한 cert-manager 설정

## 사전 준비

### cert-manager 설치

```bash
# cert-manager 설치
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.3/cert-manager.yaml

# 설치 완료 대기
kubectl wait --for=condition=ready pod -l app.kubernetes.io/instance=cert-manager -n cert-manager --timeout=300s

# 확인
kubectl get pods -n cert-manager
```

## 배포

```bash
# ClusterIssuer 배포
kubectl apply -k deploy/cert-manager/

# 또는 개별 파일
kubectl apply -f deploy/cert-manager/cluster-issuer-prod.yaml
```

## 확인

```bash
# ClusterIssuer 확인
kubectl get clusterissuer

# Certificate 상태 확인
kubectl get certificate -n book-n-stay
kubectl describe certificate -n book-n-stay

# Secret 생성 확인
kubectl get secret book-n-stay-tls -n book-n-stay

# cert-manager 로그 (문제 발생 시)
kubectl logs -n cert-manager -l app=cert-manager -f
```

## 주의사항

1. **도메인 DNS 설정 필수**: 도메인이 k3s 서버 IP를 가리켜야 함
2. **포트 80 열려있어야 함**: Let's Encrypt HTTP-01 챌린지에 필요
3. **Staging 먼저 테스트**: Production은 rate limit 있음 (주당 50개)
4. **자동 갱신**: cert-manager가 만료 30일 전에 자동으로 갱신
