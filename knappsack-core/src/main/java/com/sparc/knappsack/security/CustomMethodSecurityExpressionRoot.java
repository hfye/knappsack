package com.sparc.knappsack.security;

import com.sparc.knappsack.components.entities.*;
import com.sparc.knappsack.components.services.ApplicationVersionService;
import com.sparc.knappsack.components.services.CategoryService;
import com.sparc.knappsack.components.services.GroupService;
import com.sparc.knappsack.components.services.UserService;
import com.sparc.knappsack.enums.ApplicationType;
import com.sparc.knappsack.enums.DomainType;
import com.sparc.knappsack.enums.UserRole;
import com.sparc.knappsack.exceptions.EntityNotFoundException;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.FilterInvocation;

import java.util.Arrays;
import java.util.List;

public class CustomMethodSecurityExpressionRoot extends SecurityExpressionRoot {

    private GroupService groupService;

    private UserService userService;

    private ApplicationVersionService applicationVersionService;

    private CategoryService categoryService;

    /**
     * @param authentication Authentication
     */
    public CustomMethodSecurityExpressionRoot(Authentication authentication) {
        super(authentication);
    }

    public CustomMethodSecurityExpressionRoot(Authentication authentication, FilterInvocation fi) {
        super(authentication);
    }

    @SuppressWarnings("unused")
    public boolean hasGroupAdminAccess(Long id) {
        //Check for Admin Access for the Organization that this group belongs to
        return false;
    }

    @SuppressWarnings("unused")
    public boolean isGroupAdmin(Long id) {
        return id != null && isUserInDomain(id, DomainType.GROUP, UserRole.ROLE_GROUP_ADMIN);
    }

    @SuppressWarnings("unused")
    public boolean isOrganizationAdmin() {
        return getUser() != null && getUser().isOrganizationAdmin();
    }

    @SuppressWarnings("unused")
    public boolean isOrganizationAdmin(Long id) {
        return isUserInDomain(id, DomainType.ORGANIZATION, UserRole.ROLE_ORG_ADMIN);
    }

    @SuppressWarnings("unused")
    public boolean isOrganizationAdminForGroup(Long groupId) {
        Group group = groupService.get(groupId);
        if (group == null) {
            throw new EntityNotFoundException();
        }
        Long organizationId = group.getOrganization().getId();
        return isUserInDomain(organizationId, DomainType.ORGANIZATION, UserRole.ROLE_ORG_ADMIN);
    }

    @SuppressWarnings("unused")
    public boolean hasAccessToApplicationVersion(Long applicationVersionId) {
        User user = getUser();
        if(user == null) {
            return false;
        }

        List<ApplicationVersion> applicationVersions = userService.getApplicationVersions(user);
        for (ApplicationVersion applicationVersion : applicationVersions) {
            if(applicationVersion.getId().equals(applicationVersionId)) {
                return true;
            }
        }

        return false;
    }

    @SuppressWarnings("unused")
    public boolean hasAccessToApplication(Long applicationId) {
        User user = getUser();
        if(user == null) {
            return false;
        }

        List<ApplicationVersion> applicationVersions = userService.getApplicationVersions(user);
        for (ApplicationVersion applicationVersion : applicationVersions) {
            if(applicationVersion.getApplication().getId().equals(applicationId)) {
                return true;
            }
        }

        return false;
    }

    @SuppressWarnings("unused")
    public boolean hasUserDomainAccess(UserDomain userDomain) {
        if (userDomain != null) {
            switch (userDomain.getDomainType()) {
                case GROUP:
                    return isUserInDomain(userDomain.getDomainId(), DomainType.GROUP, UserRole.ROLE_GROUP_ADMIN, UserRole.ROLE_ORG_ADMIN);
                case ORGANIZATION:
                    return isUserInDomain(userDomain.getDomainId(), DomainType.ORGANIZATION, UserRole.ROLE_ORG_ADMIN);
            }
        }

        return false;
    }

    @SuppressWarnings("unused")
    public boolean isDomainAdmin(Long domainId, DomainType domainType) {
        return isUserInDomain(domainId, domainType, UserRole.ROLE_GROUP_ADMIN, UserRole.ROLE_ORG_ADMIN);
    }

    @SuppressWarnings("unused")
    public boolean hasDomainConfigurationAccess(Long domainId, DomainType domainType) {
        User user = getUser();
        if(user == null) {
            return false;
        }

        switch (domainType) {
            case GROUP:
                return isUserInDomain(domainId, DomainType.GROUP, UserRole.ROLE_ORG_ADMIN) || user.isSystemAdmin();
            case ORGANIZATION:
                return user.isSystemAdmin();
        }

        return false;
    }



    public boolean canEditApplication(Long applicationId) {
        User user = getUser();
        if(user == null) {
            return false;
        }

        return userService.canUserEditApplication(user.getId(), applicationId);
    }

    @SuppressWarnings("unused")
    public boolean canEditApplicationVersion(Long applicationVersionId) {
        User user = getUser();
        if (user == null) {
            return false;
        }
        ApplicationVersion applicationVersion = applicationVersionService.get(applicationVersionId);
        if (applicationVersion == null || applicationVersion.getApplication() == null) {
            throw new EntityNotFoundException();
        }

        return canEditApplication(applicationVersion.getApplication().getId());
    }

    @SuppressWarnings("unused")
    public boolean hasAccessToCategory(Long categoryId, ApplicationType applicationType) {
        User user = getUser();
        if (user == null) {
            return false;
        }

        for (Category category : userService.getCategoriesForUser(user, applicationType)) {
            if (category.getId().equals(categoryId)) {
                return true;
            }
        }

        return false;
    }

    private boolean isUserInDomain(Long id, DomainType domainType, UserRole... userRoles) {
        if (id != null && id > 0) {
            User user = getUser();

            if (user != null) {
                for (UserDomain userDomain : user.getUserDomains()) {
                    if (id.equals(userDomain.getDomainId())
                            && domainType.equals(userDomain.getDomainType())
                            && Arrays.asList(userRoles).contains(UserRole.valueOf(userDomain.getRole().getAuthority()))) {
                        return true;
                    } else if(DomainType.GROUP.equals(domainType) && isOrganizationAdminForGroup(id)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public void setGroupService(GroupService groupService) {
        this.groupService = groupService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setApplicationVersionService(ApplicationVersionService applicationVersionService) {
        this.applicationVersionService = applicationVersionService;
    }

    public void setCategoryService(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    private User getUser() {
        return userService.getUserFromSecurityContext();
    }
}
