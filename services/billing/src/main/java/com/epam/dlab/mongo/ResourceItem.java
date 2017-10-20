/***************************************************************************

Copyright (c) 2016, EPAM SYSTEMS INC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

****************************************************************************/

package com.epam.dlab.mongo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;

/** The resource of DLab environment.
 */
@Data
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"resourceName"})
public class ResourceItem implements Comparable<ResourceItem> {
	
	/** Resource ID. */
	String resourceId;
	
	/** User friendly name of resource.*/
	String resourceName;
	
	/** Type of resource. */
	DlabResourceType type;
	
	/** Name of user. */
	String user;
	
	/** Name of exploratory.*/
	String exploratoryName;

	@Override
	public int compareTo(ResourceItem o) {
		if (o == null) {
			return -1;
		}
		int result = StringUtils.compare(resourceId, o.resourceId);
		if (result == 0) {
			result = StringUtils.compare(exploratoryName, o.exploratoryName);
			if (result == 0) {
				result = StringUtils.compare(type.name(), o.type.name());
				if (result == 0) {
					return StringUtils.compare(user, o.user);
				}
			}
		}
		return result;
	}

}
