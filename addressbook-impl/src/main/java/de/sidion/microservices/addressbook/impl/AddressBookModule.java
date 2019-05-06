package de.sidion.microservices.addressbook.impl;

import com.google.inject.AbstractModule;
import com.lightbend.lagom.javadsl.server.ServiceGuiceSupport;
import de.sidion.microservices.addressbook.api.AddressBookService;

/**
 * The module that binds the AddressBookService so that it can be served.
 */
public class AddressBookModule extends AbstractModule implements ServiceGuiceSupport {
  @Override
  protected void configure() {
    bindService(AddressBookService.class, AddressBookServiceImpl.class);
  }
}
