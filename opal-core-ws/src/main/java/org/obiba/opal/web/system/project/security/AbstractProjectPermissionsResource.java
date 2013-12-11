/*
 * Copyright (c) 2013 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.opal.web.system.project.security;

import java.util.Collection;

import org.obiba.opal.core.service.security.SubjectAclService;
import org.obiba.opal.web.support.InvalidRequestException;

public abstract class AbstractProjectPermissionsResource {

  protected void setPermission(Collection<String> principals, SubjectAclService.SubjectType type, String permission) {
    validatePrincipals(principals);

    for(String principal : principals) {
      SubjectAclService.Subject subject = type.subjectFor(principal);
      getSubjectAclService().deleteSubjectPermissions(ProjectPermissionsResource.DOMAIN, getNode(), subject);
      getSubjectAclService().addSubjectPermission(ProjectPermissionsResource.DOMAIN, getNode(), subject, permission);
    }
  }

  protected void deletePermissions(Collection<String> principals, SubjectAclService.SubjectType type) {
    validatePrincipals(principals);

    for(String principal : principals) {
      SubjectAclService.Subject subject = type.subjectFor(principal);
      getSubjectAclService().deleteSubjectPermissions(ProjectPermissionsResource.DOMAIN, getNode(), subject);
    }
  }

  private void validatePrincipals(Collection<String> principals) {
    if(principals == null || principals.isEmpty()) throw new InvalidRequestException("A principal is required.");
  }

  protected abstract SubjectAclService getSubjectAclService();

  protected abstract String getNode();

}