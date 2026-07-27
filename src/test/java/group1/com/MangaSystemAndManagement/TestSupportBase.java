package group1.com.MangaSystemAndManagement;

import group1.com.MangaSystemAndManagement.model.Account;
import group1.com.MangaSystemAndManagement.model.SystemRole;
import group1.com.MangaSystemAndManagement.model.SystemRoleName;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Test helper — build an Account with a given role without persisting.
 * Uses reflection to set JPA-managed fields that have no setter exposed.
 */
public final class TestSupportBase {
    private TestSupportBase() {}

    public static Account accountWithRole(long id, SystemRoleName... roles) {
        Account a = new Account();
        setField(a, "id", id);
        setField(a, "email", "user" + id + "@test.com");
        setField(a, "firstName", "Test");
        setField(a, "lastName", "User" + id);
        setField(a, "password", "test");
        List<SystemRole> roleList = new java.util.ArrayList<>();
        for (int i = 0; i < roles.length; i++) {
            SystemRole r = new SystemRole();
            setField(r, "id", i + 1L);
            r.setRoleName(roles[i].name());
            roleList.add(r);
        }
        a.setSystemRole(roleList);
        return a;
    }

    public static void setField(Object target, String fieldName, Object value) {
        try {
            Field f = findField(target.getClass(), fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Cannot set field " + fieldName, e);
        }
    }

    private static Field findField(Class<?> c, String name) throws NoSuchFieldException {
        Class<?> cur = c;
        while (cur != null) {
            try {
                return cur.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                cur = cur.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}