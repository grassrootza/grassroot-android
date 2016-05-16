package com.techmorphosis.grassroot.models;

/**
 * Created by karthik on 12-09-2015.
 */
public class FAQModel {

    public String faq_question;
    public String faq_answer;


    public FAQModel() {

    }

    public String getFaq_question() {
        return faq_question;
    }

    public String getFaq_answer() {
        return faq_answer;
    }

    public void setFaq_question(String faq_question) {
        this.faq_question = faq_question;
    }

    public void setFaq_answer(String faq_answer) {
        this.faq_answer = faq_answer;
    }
}
