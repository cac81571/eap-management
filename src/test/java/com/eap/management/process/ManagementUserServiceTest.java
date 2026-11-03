package com.eap.management.process;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class ManagementUserServiceTest {
    @Test
    void createArgsUsesSilentManagementRealm() {
        List<String> args = ManagementUserService.createArgs("admin", "secret");
        assertEquals(List.of("-u", "admin", "-p", "secret", "-r", "ManagementRealm", "-s", "-cw"), args);
        assertTrue(args.contains("-s"));
        assertTrue(args.contains("-cw"));
    }
}
