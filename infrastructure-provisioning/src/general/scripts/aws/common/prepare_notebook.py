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

import logging
import json
import sys
from dlab.fab import *
from dlab.aws_meta import *
from dlab.aws_actions import *
import os
import uuid


if __name__ == "__main__":
    # enable debug level for boto3
    logging.getLogger('botocore').setLevel(logging.DEBUG)
    logging.getLogger('boto3').setLevel(logging.DEBUG)

    instance_class = 'notebook'
    local_log_filename = "{}_{}_{}.log".format(os.environ['resource'], os.environ['edge_user_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    # generating variables dictionary
    create_aws_config_files()
    print 'Generating infrastructure names and tags'
    notebook_config = dict()
    notebook_config['uuid'] = str(uuid.uuid4())[:5]
    try:
        notebook_config['exploratory_name'] = os.environ['exploratory_name']
    except:
        notebook_config['exploratory_name'] = ''
    notebook_config['service_base_name'] = os.environ['conf_service_base_name']
    notebook_config['instance_type'] = os.environ['aws_notebook_instance_type']
    notebook_config['key_name'] = os.environ['conf_key_name']
    notebook_config['user_keyname'] = os.environ['edge_user_name']
    notebook_config['instance_name'] = os.environ['conf_service_base_name'] + "-" + os.environ[
        'edge_user_name'] + "-nb-" + notebook_config['exploratory_name'] + "-" + notebook_config['uuid']
    notebook_config['expected_ami_name'] = os.environ['conf_service_base_name'] + "-" + os.environ[
        'edge_user_name'] + '-' + os.environ['application'] + '-notebook-image'
    notebook_config['role_profile_name'] = os.environ['conf_service_base_name'].lower().replace('-', '_') + "-" + \
                                           os.environ['edge_user_name'] + "-nb-Profile"
    notebook_config['security_group_name'] = os.environ['conf_service_base_name'] + "-" + os.environ[
        'edge_user_name'] + "-nb-SG"
    notebook_config['tag_name'] = notebook_config['service_base_name'] + '-Tag'

    print 'Searching preconfigured images'
    ami_id = get_ami_id_by_name(notebook_config['expected_ami_name'], 'available')
    if ami_id != '':
        print 'Preconfigured image found. Using: ' + ami_id
        notebook_config['ami_id'] = ami_id
    else:
        if os.environ['conf_os_family'] == "ubuntu":
            notebook_config['ami_id'] = get_ami_id(os.environ['aws_debian_ami_name'])
        if os.environ['conf_os_family'] == "redhat":
            notebook_config['ami_id'] = get_ami_id(os.environ['aws_redhat_ami_name'])
        print 'No preconfigured image found. Using default one: ' + notebook_config['ami_id']

    tag = {"Key": notebook_config['tag_name'],
           "Value": "{}-{}-subnet".format(notebook_config['service_base_name'], os.environ['edge_user_name'])}
    notebook_config['subnet_cidr'] = get_subnet_by_tag(tag)

    # launching instance for notebook server
    try:
        logging.info('[CREATE NOTEBOOK INSTANCE]')
        print '[CREATE NOTEBOOK INSTANCE]'
        params = "--node_name {} --ami_id {} --instance_type {} --key_name {} --security_group_ids {} --subnet_id {} --iam_profile {} --infra_tag_name {} --infra_tag_value {} --instance_class {} --instance_disk_size {}" \
            .format(notebook_config['instance_name'], notebook_config['ami_id'], notebook_config['instance_type'],
                    notebook_config['key_name'], get_security_group_by_name(notebook_config['security_group_name']),
                    get_subnet_by_cidr(notebook_config['subnet_cidr']), notebook_config['role_profile_name'],
                    notebook_config['tag_name'], notebook_config['instance_name'], instance_class,
                    os.environ['notebook_disk_size'])
        try:
            local("~/scripts/{}.py {}".format('create_instance', params))
        except:
            append_result("Failed to create instance")
            raise Exception
    except:
        sys.exit(1)