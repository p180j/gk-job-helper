package com.gk.jobhelper.service;
import org.springframework.scheduling.annotation.Scheduled;import org.springframework.stereotype.Component;
@Component public class RecruitmentDiscoveryScheduler {private final RecruitmentDiscoveryService service;public RecruitmentDiscoveryScheduler(RecruitmentDiscoveryService s){service=s;}@Scheduled(cron="${recruitment.discovery.cron:0 0 8 * * ?}")public void discoverDaily(){service.discoverAll();}}
