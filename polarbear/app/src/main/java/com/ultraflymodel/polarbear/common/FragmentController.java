package com.ultraflymodel.polarbear.common;


public interface FragmentController<T>
{
    public void clearAndBackToHome();

    public void loadMainPage();

    public void loadFragment(T f, String FragTag);

}
