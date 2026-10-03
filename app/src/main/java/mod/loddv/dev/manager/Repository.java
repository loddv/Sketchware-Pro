package mod.loddv.dev.manager;

import android.view.View;

/**
 * Data class representing a library repository configuration.
 */
public class Repository {
    public String name;
    public String url;
    public int menuExpanded = View.GONE;

    public Repository() {
    }

    public Repository(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getMenuExpanded() {
        return menuExpanded;
    }

    public void setMenuExpanded(int menuExpanded) {
        this.menuExpanded = menuExpanded;
    }
}
