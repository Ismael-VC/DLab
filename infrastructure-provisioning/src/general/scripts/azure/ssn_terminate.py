#!/usr/bin/python

# *****************************************************************************
#
# Copyright (c) 2016, EPAM SYSTEMS INC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# ******************************************************************************

from dlab.fab import *
from dlab.actions_lib import *
import sys, os
from fabric.api import *
from dlab.ssn_lib import *


def terminate_ssn_node(resource_group_name, service_base_name, vpc_name, region):
    print "Terminating instances"
    try:
        for vm in AzureMeta().compute_client.virtual_machines.list(resource_group_name):
            if service_base_name in vm.name:
                AzureActions().remove_instance(resource_group_name, vm.name)
                print "Instance {} has been terminated".format(vm.name)
    except:
        sys.exit(1)

    print "Removing network interfaces"
    try:
        for network_interface in AzureMeta().list_network_interfaces(resource_group_name):
            if service_base_name in network_interface.name:
                AzureActions().delete_network_if(resource_group_name, network_interface.name)
                print "Network interface {} has been removed".format(network_interface.name)
    except:
        sys.exit(1)

    print "Removing static public IPs"
    try:
        for static_public_ip in AzureMeta().list_static_ips(resource_group_name):
            if service_base_name in static_public_ip.name:
                AzureActions().delete_static_public_ip(resource_group_name, static_public_ip.name)
                print "Static public IP {} has been removed".format(static_public_ip.name)
    except:
        sys.exit(1)

    print "Removing disks"
    try:
        for disk in AzureMeta().list_disks(resource_group_name):
            if service_base_name in disk.name:
                AzureActions().remove_disk(resource_group_name, disk.name)
                print "Disk {} has been removed".format(disk.name)
    except:
        sys.exit(1)

    print "Removing storage accounts"
    try:
        for storage_account in AzureMeta().list_storage_accounts(resource_group_name):
            if service_base_name in storage_account.tags["account_name"]:
                AzureActions().remove_storage_account(resource_group_name, storage_account.name)
                print "Storage account {} has been terminated".format(storage_account.name)
    except:
        sys.exit(1)

    print "Removing security groups"
    try:
        for sg in AzureMeta().network_client.network_security_groups.list(resource_group_name):
            if service_base_name in sg.name:
                AzureActions().remove_security_group(resource_group_name, sg.name)
                print "Security group {} has been terminated".format(sg.name)
    except:
        sys.exit(1)

    print "Removing private subnets"
    try:
        for subnet in AzureMeta().network_client.subnets.list(resource_group_name, vpc_name):
            if service_base_name in subnet.name:
                AzureActions().remove_subnet(resource_group_name, vpc_name, subnet.name)
                print "Private subnet {} has been terminated".format(subnet.name)
    except:
        sys.exit(1)

    print "Removing VPC"
    try:
        if AzureMeta().get_vpc(resource_group_name, service_base_name + '-ssn-vpc'):
            AzureActions().remove_vpc(resource_group_name, vpc_name)
            print "VPC {} has been terminated".format(vpc_name)
    except:
        sys.exit(1)

    print "Removing Resource Group"
    try:
        if AzureMeta().get_resource_group(service_base_name):
            AzureActions().remove_resource_group(service_base_name, region)
            print "Resource group {} has been terminated".format(vpc_name)
    except:
        sys.exit(1)


if __name__ == "__main__":
    local_log_filename = "{}_{}.log".format(os.environ['conf_resource'], os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)
    # generating variables dictionary
    print 'Generating infrastructure names and tags'
    ssn_conf = dict()
    ssn_conf['service_base_name'] = os.environ['conf_service_base_name'].replace('_', '-')
    ssn_conf['resource_group_name'] = os.environ['azure_resource_group_name']
    ssn_conf['region'] = os.environ['azure_region']
    ssn_conf['vpc_name'] = os.environ['azure_vpc_name']

    try:
        logging.info('[TERMINATE SSN]')
        print '[TERMINATE SSN]'
        try:
            terminate_ssn_node(ssn_conf['resource_group_name'], ssn_conf['service_base_name'], ssn_conf['vpc_name'],
                               ssn_conf['region'])
        except:
            traceback.print_exc()
            raise Exception
    except Exception as err:
        append_result("Failed to terminate ssn.", str(err))
        sys.exit(1)

    try:
        with open("/root/result.json", 'w') as result:
            res = {"service_base_name": ssn_conf['service_base_name'],
                   "Action": "Terminate ssn with all service_base_name environment"}
            print json.dumps(res)
            result.write(json.dumps(res))
    except:
        print "Failed writing results."
        sys.exit(0)