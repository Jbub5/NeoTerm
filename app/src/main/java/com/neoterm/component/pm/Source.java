package com.neoterm.component.pm;

import com.neoterm.framework.database.annotation.ID;
import com.neoterm.framework.database.annotation.Table;

/**
 * @author kiva
 */
@Table
public class Source {
  @ID(autoIncrement = true)
  private int id;

  public String url;

  public String repo;

  public boolean enabled;

  public Source() {
    // for Database
  }

  public Source(String url, String repo, boolean enabled) {
    this.url = url;
    this.repo = repo;
    this.enabled = enabled;
  }
}
