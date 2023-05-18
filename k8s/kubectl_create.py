import sys, yaml, time
from kubernetes import client
from kubernetes import config
from kubernetes.client import CoreV1Api, AppsV1Api, NetworkingV1Api

branch = sys.argv[2]
namespace = sys.argv[3]
config.load_kube_config(sys.argv[1], namespace)

try:
	CoreV1Api().delete_namespaced_service('k8-pod-healthcheck', namespace)
except Exception as error:
	print(error)

try:
	AppsV1Api().delete_namespaced_deployment('k8-pod-healthcheck', namespace, propagation_policy='Foreground')
except Exception as error:
	print(error)

try:
	NetworkingV1Api().delete_namespaced_network_policy('k8-pod-healthcheck', namespace)
except Exception as error:
	print(error)

with open("service.yaml", "r") as f:
    service = yaml.load(f, Loader=yaml.FullLoader)

for i in range(100):
	try:
		print('Creating service [%d]' % i)
		CoreV1Api().create_namespaced_service(namespace, service)
		break
	except client.rest.ApiException as error:
		print(error)
		if error.reason != "Conflict":
			break
		time.sleep(2)

with open("deployment.yaml", "r") as f:
    deployment = yaml.load(f, Loader=yaml.FullLoader)
    deployment['spec']['template']['spec']['containers'][0]['image'] += ":" + branch

for i in range(100):
	try:
		print('Creating deployment [%d]' % i)
		AppsV1Api().create_namespaced_deployment(namespace, deployment)
		break
	except client.rest.ApiException as error:
		print(error)
		if error.reason != "Conflict":
			break
		time.sleep(2)

with open("network_policy.yaml") as f:
    network_policy = yaml.load(f, Loader=yaml.FullLoader)

for i in range(100):
	try:
		print('Creating network policy [%d]' % i)
		NetworkingV1Api().create_namespaced_network_policy(namespace, network_policy)
		break
	except client.rest.ApiException as error:
		print(error)
		if error.reason != "Conflict":
			break
		time.sleep(2)
