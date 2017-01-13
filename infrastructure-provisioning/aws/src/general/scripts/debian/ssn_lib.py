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

import json
import sys
import crypt
import yaml
from fabric.api import *
from fabric.contrib.files import exists
import os


def ensure_docker_daemon():
    try:
        if not exists(os.environ['ssn_dlab_path'] + 'tmp/docker_daemon_ensured'):
            sudo('apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 58118E89F3A912897C070ADBF76221572C52609D')
            sudo('echo "deb https://apt.dockerproject.org/repo ubuntu-xenial main" | sudo tee /etc/apt/sources.list.d/docker.list')
            sudo('apt-get update')
            sudo('apt-cache policy docker-engine')
            sudo('apt-get install -y docker-engine')
            sudo('usermod -a -G docker ubuntu')
            sudo('sysv-rc-conf docker on')
            sudo('touch ' + os.environ['ssn_dlab_path'] + 'tmp/docker_daemon_ensured')
        return True
    except:
        return False


def add_2_yml_config(path,section,param,value):
    try:
        try:
            with open(path, 'r') as config_yml_r:
                config_orig = yaml.load(config_yml_r)
        except:
            config_orig = {}
        sections = []
        for i in config_orig:
            sections.append(i)
        if section in sections:
            config_orig[section].update({param:value})
        else:
            config_orig.update({section:{param:value}})
        with open(path, 'w') as outfile_yml_w:
            yaml.dump(config_orig, outfile_yml_w, default_flow_style=True)
        return True
    except:
        print "Could not write the target file"
        return False


def cp_key(keyfile, host_string):
    try:
        key_name=keyfile.split("/")
        sudo('mkdir -p /home/ubuntu/keys')
        sudo('chown -R ubuntu:ubuntu /home/ubuntu/keys')
        local('scp -r -q -i {0} {0} {1}:/home/ubuntu/keys/{2}'.format(keyfile, host_string, key_name[-1]))
        sudo('chmod 600 /home/ubuntu/keys/*.pem')
        return True
    except:
        return False


def ensure_nginx():
    try:
        if not exists(os.environ['ssn_dlab_path'] + 'tmp/nginx_ensured'):
            sudo('apt-get -y install nginx')
            sudo('service nginx restart')
            sudo('update-rc.d nginx defaults')
            sudo('update-rc.d nginx enable')
            sudo('touch ' + os.environ['ssn_dlab_path'] + 'tmp/nginx_ensured')
        return True
    except:
        return False


def ensure_jenkins():
    try:
        if not exists(os.environ['ssn_dlab_path'] + 'tmp/jenkins_ensured'):
            sudo('wget -q -O - https://pkg.jenkins.io/debian/jenkins-ci.org.key | apt-key add -')
            sudo('echo deb http://pkg.jenkins.io/debian-stable binary/ > /etc/apt/sources.list.d/jenkins.list')
            sudo('apt-get -y update')
            sudo('apt-get -y install jenkins')
            sudo('touch ' + os.environ['ssn_dlab_path'] + 'tmp/jenkins_ensured')
        return True
    except:
        return False


def configure_jenkins():
    try:
        if not exists(os.environ['ssn_dlab_path'] + 'tmp/jenkins_configured'):
            sudo('echo \'JENKINS_ARGS="--prefix=/jenkins --httpPort=8070"\' >> /etc/default/jenkins')
            sudo('rm -rf /var/lib/jenkins/*')
            sudo('mkdir -p /var/lib/jenkins/jobs/')
            sudo('chown -R ubuntu:ubuntu /var/lib/jenkins/')
            put('/root/templates/jenkins_jobs/*', '/var/lib/jenkins/jobs/')
            sudo('chown -R jenkins:jenkins /var/lib/jenkins')
            sudo('/etc/init.d/jenkins stop; sleep 5')
            sudo('sysv-rc-conf jenkins on')
            sudo('service jenkins start')
            sudo('touch ' + os.environ['ssn_dlab_path'] + '/tmp/jenkins_configured')
            sudo('echo "jenkins ALL = NOPASSWD:ALL" >> /etc/sudoers')
        return True
    except:
        return False


def creating_service_directories():
    try:
        if not exists(os.environ['ssn_dlab_path']):
            sudo('mkdir -p ' + os.environ['ssn_dlab_path'])
            sudo('mkdir -p ' + os.environ['ssn_dlab_path'] + 'conf')
            sudo('mkdir -p ' + os.environ['ssn_dlab_path'] + 'webapp/lib')
            sudo('mkdir -p ' + os.environ['ssn_dlab_path'] + 'webapp/static')
            sudo('mkdir -p ' + os.environ['ssn_dlab_path'] + 'template')
            sudo('mkdir -p ' + os.environ['ssn_dlab_path'] + 'tmp')
            sudo('mkdir -p ' + os.environ['ssn_dlab_path'] + 'tmp/result')
            sudo('mkdir -p /var/opt/dlab/log/ssn')
            sudo('mkdir -p /var/opt/dlab/log/edge')
            sudo('mkdir -p /var/opt/dlab/log/notebook')
            sudo('mkdir -p /var/opt/dlab/log/emr')
            sudo('ln -s ' + os.environ['ssn_dlab_path'] + 'conf /etc/opt/dlab')
            sudo('ln -s /var/opt/dlab/log /var/log/dlab')
            sudo('chown -R ubuntu:ubuntu /var/opt/dlab/log')
            sudo('chown -R ubuntu:ubuntu ' + os.environ['ssn_dlab_path'])

        return True
    except:
        return False


def configure_nginx(config):
    try:
        random_file_part = id_generator(size=20)
        if not exists("/etc/nginx/conf.d/nginx_proxy.conf"):
            sudo('rm -f /etc/nginx/conf.d/*')
            put(config['nginx_template_dir'] + 'nginx_proxy.conf', '/tmp/nginx_proxy.conf')
            sudo('mv /tmp/nginx_proxy.conf ' + os.environ['ssn_dlab_path'] + 'tmp/')
            sudo('\cp ' + os.environ['ssn_dlab_path'] + 'tmp/nginx_proxy.conf /etc/nginx/conf.d/')
            sudo('mkdir -p /etc/nginx/locations')
            sudo('rm -f /etc/nginx/sites-enabled/default')
    except:
        return False

    try:
        if not exists("/etc/nginx/locations/proxy_location_jenkins.conf"):
            nginx_password = id_generator()
            template_file = config['nginx_template_dir'] + 'proxy_location_jenkins_template.conf'
            with open("/tmp/%s-tmpproxy_location_jenkins_template.conf" % random_file_part, 'w') as out:
                with open(template_file) as tpl:
                    for line in tpl:
                        out.write(line)
            put("/tmp/%s-tmpproxy_location_jenkins_template.conf" % random_file_part, '/tmp/proxy_location_jenkins.conf')
            sudo('mv /tmp/proxy_location_jenkins.conf ' + os.environ['ssn_dlab_path'] + 'tmp/')
            sudo('\cp ' + os.environ['ssn_dlab_path'] + 'tmp/proxy_location_jenkins.conf /etc/nginx/locations/')
            sudo("echo 'engineer:" + crypt.crypt(nginx_password, id_generator()) + "' > /etc/nginx/htpasswd")
            with open('jenkins_crids.txt', 'w+') as f:
                f.write("Jenkins credentials: engineer  / " + nginx_password)
    except:
        return False

    try:
        sudo('service nginx reload')
        return True
    except:
        return False


def ensure_supervisor():
    try:
        if not exists(os.environ['ssn_dlab_path'] + 'tmp/superv_ensured'):
            sudo('apt-get -y install supervisor')
            #sudo('sysv-rc-conf supervisor on')
            sudo('update-rc.d supervisor defaults')
            sudo('update-rc.d supervisor enable')
            sudo('touch ' + os.environ['ssn_dlab_path'] + 'tmp/superv_ensured')
        return True
    except:
        return False


def ensure_mongo():
    try:
        if not exists(os.environ['ssn_dlab_path'] + 'tmp/mongo_ensured'):
            sudo('apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv EA312927')
            sudo('ver=`lsb_release -cs`; echo "deb http://repo.mongodb.org/apt/ubuntu $ver/mongodb-org/3.2 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-3.2.list; apt-get update')
            sudo('apt-get -y install mongodb-org')
            sudo('systemctl enable mongod.service')
            sudo('touch ' + os.environ['ssn_dlab_path'] + 'tmp/mongo_ensured')
        return True
    except:
        return False


def start_ss(keyfile, host_string):
    try:
        if not exists(os.environ['ssn_dlab_path'] + 'tmp/ss_started'):
            supervisor_conf = '/etc/supervisor/conf.d/supervisor_svc.conf'
            put('/root/templates/ssn.yml', '/tmp/ssn.yml')
            sudo('mv /tmp/ssn.yml ' + os.environ['ssn_dlab_path'] + 'conf/')
            put('/root/templates/proxy_location_webapp_template.conf', '/tmp/proxy_location_webapp_template.conf')
            sudo('mv /tmp/proxy_location_webapp_template.conf ' + os.environ['ssn_dlab_path'] + 'tmp/')
            with open('/root/templates/supervisor_svc.conf', 'r') as f:
                text = f.read()
            text = text.replace('WEB_CONF', dlab_conf_dir)
            with open('/root/templates/supervisor_svc.conf', 'w') as f:
                f.write(text)
            put('/root/templates/supervisor_svc.conf', '/tmp/supervisor_svc.conf')
            sudo('mv /tmp/supervisor_svc.conf ' + os.environ['ssn_dlab_path'] + 'tmp/')
            sudo('cp ' + os.environ['ssn_dlab_path'] + 'tmp/proxy_location_webapp_template.conf /etc/nginx/locations/proxy_location_webapp.conf')
            sudo('cp ' + os.environ['ssn_dlab_path'] + 'tmp/supervisor_svc.conf {}'.format(supervisor_conf))

            sudo('sed -i \'s=WEB_APP_DIR={}=\' {}'.format(web_path, supervisor_conf))

            sudo('mkdir -p /var/log/application')
            sudo('mkdir -p ' + web_path)
            sudo('mkdir -p ' + web_path + 'provisioning-service/')
            sudo('mkdir -p ' + web_path + 'security-service/')
            sudo('mkdir -p ' + web_path + 'self-service/')
            sudo('chown -R ubuntu:ubuntu ' + web_path)
            try:
                local('scp -r -i {} /root/web_app/self-service/*.jar {}:'.format(keyfile, host_string) + web_path + 'self-service/')
                local('scp -r -i {} /root/web_app/security-service/*.jar {}:'.format(keyfile, host_string) + web_path + 'security-service/')
                local('scp -r -i {} /root/web_app/provisioning-service/*.jar {}:'.format(keyfile, host_string) + web_path + 'provisioning-service/')
                run('mkdir -p /tmp/yml_tmp/')
                local('scp -r -i {} /root/web_app/self-service/*.yml {}:'.format(keyfile, host_string) + '/tmp/yml_tmp/')
                local('scp -r -i {} /root/web_app/security-service/*.yml {}:'.format(keyfile, host_string) + '/tmp/yml_tmp/')
                local('scp -r -i {} /root/web_app/provisioning-service/*.yml {}:'.format(keyfile, host_string) + '/tmp/yml_tmp/')
                sudo('mv /tmp/yml_tmp/* ' + os.environ['ssn_dlab_path'] + 'conf/')
                sudo('rmdir /tmp/yml_tmp/')
            except:
                with open("/root/result.json", 'w') as result:
                    res = {"error": "Unable to upload webapp jars", "conf": os.environ.__dict__}
                    print json.dumps(res)
                    result.write(json.dumps(res))
                sys.exit(1)

            sudo('service supervisor start')
            sudo('service nginx restart')
            sudo('touch ' + os.environ['ssn_dlab_path'] + 'tmp/ss_started')
        return True
    except:
        return False


def upload_response_file(instance_name, local_log_filepath):
    instance_hostname = get_instance_hostname(instance_name)
    print 'Connect to SSN instance with hostname: ' + instance_hostname + 'and name: ' + instance_name
    env['connection_attempts'] = 100
    env.key_filename = "/root/keys/%s.pem" % os.environ['creds_key_name']
    env.host_string = 'ubuntu@' + instance_hostname
    try:
        put('/root/result.json', '/home/ubuntu/%s.json' % os.environ['request_id'])
        sudo('mv /home/ubuntu/' + os.environ['request_id'] + '.json ' + os.environ['ssn_dlab_path'] + 'tmp/result/')
        put(local_log_filepath, '/home/ubuntu/ssn.log')
        sudo('mv /home/ubuntu/ssn.log /var/opt/dlab/log/ssn/')
    except:
        print 'Failed to upload response file'
        sys.exit(1)