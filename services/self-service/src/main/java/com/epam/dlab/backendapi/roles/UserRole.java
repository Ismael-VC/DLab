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

package com.epam.dlab.backendapi.roles;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Describe role.
 */
@Data
@AllArgsConstructor
public class UserRole implements Comparable<UserRole> {

	/** Type of role. */
	private final RoleType type;
	
	/** Name of role. */
	private final String name;
	
	/** Names of external groups. */
	private final Set<String> groups;
	
	/** Name of DLab's users. */
	private final Set<String> users;

	@Override
	public int compareTo(UserRole o) {
		int result = type.compareTo(o.type);
		return (result == 0 ? name.compareTo(o.name) : result);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof UserRole)) return false;
		UserRole that = (UserRole) o;
		return compareTo(that) == 0;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (type != null ? type.hashCode() : 0);
		result = 31 * result + (name != null ? name.hashCode() : 0);
		return result;
	}
}
